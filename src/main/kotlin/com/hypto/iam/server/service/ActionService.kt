package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.ActionPaginatedResponse
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.ResourceHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ActionServiceImpl : KoinComponent, ActionService {
    private val actionRepo: ActionRepo by inject()

    override suspend fun createAction(
        organizationId: String,
        resourceName: String,
        name: String,
        description: String
    ): Action {
        val actionHrn = ActionHrn(organizationId, null, resourceName, name)
        val resourceHrn = ResourceHrn(organizationId, null, resourceName, null)

        if (actionRepo.existsById(actionHrn.toString())) {
            throw IllegalArgumentException("Action with name [$name] already exists")
        }

        val actionRecord = actionRepo.create(organizationId, resourceHrn, actionHrn, description)

        return Action.from(actionRecord)
    }

    override suspend fun getAction(organizationId: String, resourceName: String, name: String): Action {
        val actionHrn = ActionHrn(organizationId, null, resourceName, name)

        val actionRecord =
            actionRepo.fetchByHrn(actionHrn) ?: throw EntityNotFoundException("Action with name [$name] not found")
        return Action.from(actionRecord)
    }

    override suspend fun listActions(
        organizationId: String,
        resourceName: String,
        context: PaginationContext
    ): ActionPaginatedResponse {
        val resourceHrn = ResourceHrn(organizationId, null, resourceName, null)

        val actions = actionRepo.fetchActionsPaginated(organizationId, resourceHrn, context)
        val newContext = PaginationContext.from(actions.lastOrNull()?.hrn, context)
        return ActionPaginatedResponse(
            actions.map { Action.from(it) },
            newContext.nextToken,
            newContext.toOptions()
        )
    }

    override suspend fun updateAction(
        organizationId: String,
        resourceName: String,
        name: String,
        description: String
    ): Action {
        val actionHrn = ActionHrn(organizationId, null, resourceName, name)

        val actionRecord = actionRepo.update(actionHrn, description)
            ?: throw IllegalStateException("Action cannot be updated")

        return Action.from(actionRecord)
    }

    override suspend fun deleteAction(organizationId: String, resourceName: String, name: String): BaseSuccessResponse {
        val actionHrn = ActionHrn(organizationId, null, resourceName, name)

        val response = actionRepo.delete(actionHrn)
        return BaseSuccessResponse(response)
    }
}

interface ActionService {
    suspend fun createAction(
        organizationId: String,
        resourceName: String,
        name: String,
        description: String
    ): Action

    suspend fun getAction(organizationId: String, resourceName: String, name: String): Action
    suspend fun listActions(
        organizationId: String,
        resourceName: String,
        context: PaginationContext
    ): ActionPaginatedResponse

    suspend fun updateAction(
        organizationId: String,
        resourceName: String,
        name: String,
        description: String
    ): Action

    suspend fun deleteAction(organizationId: String, resourceName: String, name: String): BaseSuccessResponse
}

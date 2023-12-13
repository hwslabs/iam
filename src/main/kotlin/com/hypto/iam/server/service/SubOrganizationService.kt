package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.SubOrganizationRepo
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.SubOrganization
import com.hypto.iam.server.models.SubOrganizationsPaginatedResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SubOrganizationServiceImpl : KoinComponent, SubOrganizationService {
    private val subOrganizationRepo: SubOrganizationRepo by inject()

    override suspend fun createSubOrganization(
        organizationId: String,
        id: String,
        name: String,
        description: String?
    ): SubOrganization {
        val subOrganizationRecord = subOrganizationRepo.create(organizationId, id, name, description)
        return SubOrganization.from(subOrganizationRecord)
    }

    override suspend fun getSubOrganization(organizationId: String, id: String): SubOrganization {
        val subOrganizationRecord =
            subOrganizationRepo.fetchById(organizationId, id) ?: throw EntityNotFoundException(
                "SubOrganization with " +
                    "id [$id] not found"
            )
        return SubOrganization.from(subOrganizationRecord)
    }

    override suspend fun listSubOrganizations(
        organizationId: String,
        context: PaginationContext
    ): SubOrganizationsPaginatedResponse {
        val subOrganizationsRecord = subOrganizationRepo.fetchSubOrganizationsPaginated(organizationId, context)
        val newContext = PaginationContext.from(subOrganizationsRecord.lastOrNull()?.id, context)
        return SubOrganizationsPaginatedResponse(
            subOrganizationsRecord.map { SubOrganization.from(it) },
            newContext.nextToken,
            newContext.toOptions()
        )
    }

    override suspend fun updateSubOrganization(
        organizationId: String,
        id: String,
        updatedName: String?,
        updatedDescription: String?
    ): SubOrganization {
        val subOrganizationRecord = subOrganizationRepo
            .update(organizationId, id, updatedName, updatedDescription)
            ?: throw EntityNotFoundException("SubOrganization with id [$id] not found")
        return SubOrganization.from(subOrganizationRecord)
    }

    override suspend fun deleteSubOrganization(organizationId: String, id: String): BaseSuccessResponse {
        subOrganizationRepo.delete(organizationId, id)
        return BaseSuccessResponse(true)
    }
}

interface SubOrganizationService {
    suspend fun createSubOrganization(
        organizationId: String,
        id: String,
        name: String,
        description: String?
    ): SubOrganization
    suspend fun getSubOrganization(organizationId: String, id: String): SubOrganization
    suspend fun listSubOrganizations(
        organizationId: String,
        context: PaginationContext
    ): SubOrganizationsPaginatedResponse
    suspend fun updateSubOrganization(
        organizationId: String,
        id: String,
        updatedName: String?,
        updatedDescription: String?
    ): SubOrganization
    suspend fun deleteSubOrganization(organizationId: String, id: String): BaseSuccessResponse
}

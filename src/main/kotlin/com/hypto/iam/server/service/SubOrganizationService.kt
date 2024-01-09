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
        subOrganizationName: String,
        description: String?,
    ): SubOrganization {
        val subOrganizationRecord = subOrganizationRepo.create(organizationId, subOrganizationName, description)
        return SubOrganization.from(subOrganizationRecord)
    }

    override suspend fun getSubOrganization(
        organizationId: String,
        subOrganizationName: String,
    ): SubOrganization {
        val subOrganizationRecord =
            subOrganizationRepo.fetchById(organizationId, subOrganizationName) ?: throw EntityNotFoundException(
                "SubOrganization with " +
                    "name [$subOrganizationName] not found",
            )
        return SubOrganization.from(subOrganizationRecord)
    }

    override suspend fun listSubOrganizations(
        organizationId: String,
        context: PaginationContext,
    ): SubOrganizationsPaginatedResponse {
        val subOrganizationsRecord = subOrganizationRepo.fetchSubOrganizationsPaginated(organizationId, context)
        val newContext = PaginationContext.from(subOrganizationsRecord.lastOrNull()?.name, context)
        return SubOrganizationsPaginatedResponse(
            subOrganizationsRecord.map { SubOrganization.from(it) },
            newContext.nextToken,
            newContext.toOptions(),
        )
    }

    override suspend fun updateSubOrganization(
        organizationId: String,
        subOrganizationName: String,
        updatedDescription: String?,
    ): SubOrganization {
        val subOrganizationRecord =
            subOrganizationRepo
                .update(organizationId, subOrganizationName, updatedDescription)
                ?: throw EntityNotFoundException("SubOrganization with id [$subOrganizationName] not found")
        return SubOrganization.from(subOrganizationRecord)
    }

    override suspend fun deleteSubOrganization(
        organizationId: String,
        subOrganizationName: String,
    ): BaseSuccessResponse {
        subOrganizationRepo.delete(organizationId, subOrganizationName)
        return BaseSuccessResponse(true)
    }
}

interface SubOrganizationService {
    suspend fun createSubOrganization(
        organizationId: String,
        subOrganizationName: String,
        description: String?,
    ): SubOrganization

    suspend fun getSubOrganization(
        organizationId: String,
        subOrganizationName: String,
    ): SubOrganization

    suspend fun listSubOrganizations(
        organizationId: String,
        context: PaginationContext,
    ): SubOrganizationsPaginatedResponse

    suspend fun updateSubOrganization(
        organizationId: String,
        subOrganizationName: String,
        updatedDescription: String?,
    ): SubOrganization

    suspend fun deleteSubOrganization(
        organizationId: String,
        subOrganizationName: String,
    ): BaseSuccessResponse
}

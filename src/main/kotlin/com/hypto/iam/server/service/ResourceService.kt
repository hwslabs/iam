package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Resource
import com.hypto.iam.server.models.ResourcePaginatedResponse
import com.hypto.iam.server.utils.ResourceHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ResourceServiceImpl : KoinComponent, ResourceService {
    private val resourceRepo: ResourceRepo by inject()

    override suspend fun createResource(organizationId: String, name: String, description: String): Resource {
        val resourceHrn = ResourceHrn(organizationId, null, name, null)

        if (resourceRepo.existsById(resourceHrn.toString())) {
            throw EntityAlreadyExistsException("Policy with name [$name] already exists")
        }

        val resourceRecord = resourceRepo.create(resourceHrn, description)
        return Resource.from(resourceRecord)
    }

    override suspend fun getResource(organizationId: String, name: String): Resource {
        val resourceRecord = resourceRepo.fetchByHrn(ResourceHrn(organizationId, null, name, null))
            ?: throw EntityNotFoundException("Resource with name [$name] not found")
        return Resource.from(resourceRecord)
    }

    override suspend fun listResources(
        organizationId: String,
        context: PaginationContext
    ): ResourcePaginatedResponse {
        val resources = resourceRepo.fetchByOrganizationIdPaginated(organizationId, context)
        val newContext = PaginationContext.from(resources.lastOrNull()?.hrn, context)
        return ResourcePaginatedResponse(
            resources.map { Resource.from(it) },
            newContext.nextToken,
            newContext.toOptions()
        )
    }

    override suspend fun updateResource(organizationId: String, name: String, description: String): Resource {
        val resourceHrn = ResourceHrn(organizationId, null, name, null)

        val resourceRecord = resourceRepo.update(
            resourceHrn,
            description
        )

        resourceRecord ?: throw IllegalStateException("Update unsuccessful")
        return Resource.from(resourceRecord)
    }

    override suspend fun deleteResource(organizationId: String, name: String): BaseSuccessResponse {
        val resourceHrn = ResourceHrn(organizationId, null, name, null)

        resourceRepo.delete(resourceHrn)
        return BaseSuccessResponse(true)
    }
}

/**
 * Service which holds logic related to Resource operations
 */
interface ResourceService {
    suspend fun createResource(organizationId: String, name: String, description: String): Resource
    suspend fun getResource(organizationId: String, name: String): Resource
    suspend fun updateResource(organizationId: String, name: String, description: String): Resource
    suspend fun deleteResource(organizationId: String, name: String): BaseSuccessResponse
    suspend fun listResources(
        organizationId: String,
        context: PaginationContext
    ): ResourcePaginatedResponse
}

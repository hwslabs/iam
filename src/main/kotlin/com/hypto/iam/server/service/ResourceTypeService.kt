package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.ResourceTypesRepo
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.ResourceType
import com.hypto.iam.server.utils.GlobalHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ResourceTypeServiceImpl : KoinComponent, ResourceTypeService {
    private val resourceTypeRepo: ResourceTypesRepo by inject()

    override suspend fun createResourceType(organizationId: String, name: String, description: String): ResourceType {
        val resourceTypeHrn = GlobalHrn(organizationId, name, null)

        if (resourceTypeRepo.existsById(resourceTypeHrn.toString())) {
            throw EntityAlreadyExistsException("Policy with name [$name] already exists")
        }

        val resourceTypeRecord = resourceTypeRepo.create(resourceTypeHrn, description)
        return ResourceType.from(resourceTypeRecord)

    }

    override suspend fun getResourceType(organizationId: String, name: String): ResourceType {
        val resourceTypeRecord = resourceTypeRepo.fetchByHrn(GlobalHrn(organizationId, name, null))
            ?: throw EntityNotFoundException("Resource type with name [$name] not found")
        return ResourceType.from(resourceTypeRecord)

    }

    override suspend fun updateResourceType(organizationId: String, name: String, description: String): ResourceType {
        val resourceTypeHrn = GlobalHrn(organizationId, name, null)

        val resourceTypeRecord = resourceTypeRepo.update(
            resourceTypeHrn,
            description
        )

        resourceTypeRecord ?: throw IllegalStateException("Update unsuccessful")
        return ResourceType.from(resourceTypeRecord)

    }

    override suspend fun deleteResourceType(organizationId: String, name: String): BaseSuccessResponse {
        val resourceTypeHrn = GlobalHrn(organizationId, name, null)

        if (!resourceTypeRepo.delete(resourceTypeHrn)) {
            throw EntityNotFoundException("Resource Type not found")
        }

        return BaseSuccessResponse(true)
    }

}

/**
 * Service which holds logic related to ResourceType operations
 */
interface ResourceTypeService {
    suspend fun createResourceType(organizationId: String, name: String, description: String): ResourceType
    suspend fun getResourceType(organizationId: String, name: String): ResourceType
    suspend fun updateResourceType(organizationId: String, name: String, description: String): ResourceType
    suspend fun deleteResourceType(organizationId: String, name: String): BaseSuccessResponse
}

package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.utils.ApplicationIdUtil
import io.micrometer.core.annotation.Timed
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrganizationsServiceImpl : KoinComponent, OrganizationsService {
    private val repo: OrganizationRepo by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()

    override suspend fun createOrganization(name: String, description: String): Organization {
        val id = idGenerator.organizationId()
        repo.insert(Organizations(id, name, "", "", LocalDateTime.now(), LocalDateTime.now()))

        // TODO: #1 - Update schema to include description, createdBy, updatedBy fields
        // TODO: #2 - Add transaction to create iam resource_types and actions
        // TODO: #3 - Add transaction to create admin user

        return getOrganization(id)
    }

    @Timed("organization.get") // TODO: Make this work
    override suspend fun getOrganization(id: String): Organization {
        val response = repo.findById(id) ?: throw EntityNotFoundException("Unable to get Organization")
        return Organization(
            id = response.id,
            name = response.name,
            description = ""
        )
    }

    override suspend fun updateOrganization(id: String, description: String): Organization {
        TODO("Not yet implemented")
    }

    override suspend fun deleteOrganization(id: String) {
        TODO("Not yet implemented")
    }
}

/**
 * Service which holds logic related to Organization operations
 */
interface OrganizationsService {
    suspend fun createOrganization(name: String, description: String): Organization
    suspend fun getOrganization(id: String): Organization
    suspend fun updateOrganization(id: String, description: String): Organization
    suspend fun deleteOrganization(id: String)
}

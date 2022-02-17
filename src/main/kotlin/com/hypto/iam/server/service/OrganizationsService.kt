package com.hypto.iam.server.controller

import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.utils.IdUtil
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrganizationsServiceImpl : KoinComponent, OrganizationsService {
    private val repo: OrganizationRepo by inject()
    private val idUtil: IdUtil by inject()

    override suspend fun createOrganization(name: String, description: String): Organization {
        val id = idUtil.randomId(charset = IdUtil.IdCharset.ALPHANUMERIC)
        repo.insert(Organizations(id, name, "",
            null, LocalDateTime.now(), LocalDateTime.now()))

        // TODO: #1 - Update schema to include description, createdBy, updatedBy fields
        // TODO: #2 - Add transaction to create iam resource_types and actions
        // TODO: #3 - Add transaction to create admin user

        return getOrganization(id)
    }

    override suspend fun getOrganization(id: String): Organization {
        val response = repo.findById(id) ?: throw IllegalStateException("Unable to get Organization")
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

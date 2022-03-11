package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import io.micrometer.core.annotation.Timed
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrganizationsServiceImpl : KoinComponent, OrganizationsService {
    private val organizationRepo: OrganizationRepo by inject()
    private val usersService: UsersService by inject()
    private val policyService: PolicyService by inject()
    private val hrnFactory: HrnFactory by inject()
    private val userPolicyService: UserPolicyService by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()

    override suspend fun createOrganization(name: String, description: String, adminUser: AdminUser): Organization {
        val organizationId = idGenerator.organizationId()
        val adminUserHrn = ResourceHrn(organizationId, "", IamResourceTypes.USER, adminUser.username)

        // Create Organization
        organizationRepo.insert(Organizations(organizationId,
            name,
            description,
            adminUserHrn.toString(),
            LocalDateTime.now(), LocalDateTime.now()))

        // Create admin user for the organization
        val user = usersService.createUser(organizationId = organizationId,
            userName = adminUser.username!!,
            password = adminUser.passwordHash,
            email = adminUser.email,
            userType = User.UserType.admin,
            createdBy = adminUser.username,
            status = User.Status.active, phone = adminUser.phone)

        // Add policies for the admin user
        val organization = getOrganization(organizationId)
        val policyStatements = listOf(
            PolicyStatement(organization.id, "*", PolicyStatement.Effect.allow),
            PolicyStatement("hrn:$organizationId::*", "*", PolicyStatement.Effect.allow))
        val policy = policyService.createPolicy(organizationId, "ROOT_USER_POLICY", policyStatements)
        userPolicyService.attachPoliciesToUser(hrnFactory.getHrn(user.id), listOf(hrnFactory.getHrn(policy.id)))
        return organization
    }

    @Timed("organization.get") // TODO: Make this work
    override suspend fun getOrganization(id: String): Organization {
        val response = organizationRepo.findById(id) ?: throw EntityNotFoundException("Unable to get Organization")
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
    suspend fun createOrganization(name: String, description: String, adminUser: AdminUser): Organization
    suspend fun getOrganization(id: String): Organization
    suspend fun updateOrganization(id: String, description: String): Organization
    suspend fun deleteOrganization(id: String)
}

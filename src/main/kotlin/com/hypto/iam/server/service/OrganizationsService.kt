package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.idp.PasswordCredentials
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.txman.TxMan
import io.micrometer.core.annotation.Timed
import java.time.LocalDateTime
import mu.KotlinLogging
import org.jooq.JSONB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrganizationAlreadyExistException(message: String) : Exception(message)

private val logger = KotlinLogging.logger { }

class OrganizationsServiceImpl : KoinComponent, OrganizationsService {
    private val organizationRepo: OrganizationRepo by inject()
    private val usersService: UsersService by inject()
    private val credentialService: CredentialService by inject()
    private val policyService: PolicyService by inject()
    private val hrnFactory: HrnFactory by inject()
    private val userPolicyService: UserPolicyService by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val identityProvider: IdentityProvider by inject()
    private val gson: Gson by inject()
    private val txMan: TxMan by inject()

    override suspend fun createOrganization(
        name: String,
        description: String,
        adminUser: AdminUser
    ): Pair<Organization, Credential> {
        val organizationId = idGenerator.organizationId()

        val identityGroup = identityProvider.createIdentityGroup(organizationId)

        @Suppress("TooGenericExceptionCaught")
        try {
            return txMan.wrap {
                // Create Organization
                organizationRepo.insert(
                    Organizations(
                        organizationId,
                        name,
                        description,
                        ResourceHrn(
                            organization = organizationId,
                            resource = IamResources.USER,
                            resourceInstance = adminUser.username
                        ).toString(),
                        JSONB.jsonb(gson.toJson(identityGroup)),
                        LocalDateTime.now(), LocalDateTime.now()
                    )
                )

                // Create admin user for the organization
                val user = usersService.createUser(
                    organizationId = organizationId,
                    credentials = PasswordCredentials(
                        userName = adminUser.username,
                        email = adminUser.email,
                        phoneNumber = adminUser.phone, password = adminUser.passwordHash
                    ),
                    createdBy = "iam-system"
                )

                // TODO: Avoid this duplicate call be returning the created organization from `organizationRepo.insert`
                val organization = getOrganization(organizationId)
                // Add policies for the admin user
                val policyStatements = listOf(
                    // TODO: Change organization admin's policy string to hrn:::iam-organization/$orgId
                    PolicyStatement("hrn:$organizationId", "hrn:$organizationId:*", PolicyStatement.Effect.allow),
                    PolicyStatement("hrn:$organizationId::*", "hrn:$organizationId::*", PolicyStatement.Effect.allow)
                )
                val policy = policyService.createPolicy(organizationId, "ROOT_USER_POLICY", policyStatements)
                userPolicyService.attachPoliciesToUser(
                    hrnFactory.getHrn(user.hrn),
                    listOf(hrnFactory.getHrn(policy.hrn))
                )

                val credential = credentialService.createCredential(organizationId, adminUser.username)
                return@wrap Pair(organization, credential)
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception when creating organization. Rolling back..." }

            identityProvider.deleteIdentityGroup(identityGroup)
            throw e
        }
    }

    @Timed("organization.get") // TODO: Make this work
    override suspend fun getOrganization(id: String): Organization {
        val response = organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        return Organization(
            id = response.id,
            name = response.name,
            description = response.description,
            adminUserHrn = response.adminUserHrn
        )
    }

    override suspend fun updateOrganization(id: String, name: String?, description: String?): Organization {
        organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        val updatedOrgRecord =
            organizationRepo.update(id, name, description) ?: throw InternalException("Internal service failure")
        return Organization(
            id = updatedOrgRecord.id,
            name = updatedOrgRecord.name,
            description = updatedOrgRecord.description
        )
    }

    override suspend fun deleteOrganization(id: String): BaseSuccessResponse {
        organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        organizationRepo.deleteById(id)
        return BaseSuccessResponse(true)
    }
}

/**
 * Service which holds logic related to Organization operations
 */
interface OrganizationsService {
    suspend fun createOrganization(name: String, description: String, adminUser: AdminUser):
        Pair<Organization, Credential>
    suspend fun getOrganization(id: String): Organization
    suspend fun updateOrganization(id: String, name: String?, description: String?): Organization
    suspend fun deleteOrganization(id: String): BaseSuccessResponse
}

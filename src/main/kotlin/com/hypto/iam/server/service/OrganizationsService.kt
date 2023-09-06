package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.extensions.toUTCOffset
import com.hypto.iam.server.idp.IdentityGroup
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.txman.TxMan
import io.micrometer.core.annotation.Timed
import java.time.LocalDateTime
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.JSONB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrganizationAlreadyExistException(message: String) : Exception(message)

private val logger = KotlinLogging.logger { }

class OrganizationsServiceImpl : KoinComponent, OrganizationsService {
    private val organizationRepo: OrganizationRepo by inject()
    private val passcodeRepo: PasscodeRepo by inject()
    private val usersService: UsersService by inject()
    private val tokenService: TokenService by inject()
    private val hrnFactory: HrnFactory by inject()
    private val principalPolicyService: PrincipalPolicyService by inject()
    private val policyTemplatesService: PolicyTemplatesService by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val identityProvider: IdentityProvider by inject()
    private val gson: Gson by inject()
    private val txMan: TxMan by inject()

    override suspend fun createOrganization(
        request: CreateOrganizationRequest
    ): Pair<Organization, TokenResponse> {
        val organizationId = idGenerator.organizationId()
        val username = idGenerator.username()
        val logTimestamp = LocalDateTime.now()

        val rootUserFromRequest = request.rootUser
        val identityGroup = identityProvider.createIdentityGroup(organizationId)

        @Suppress("TooGenericExceptionCaught")
        try {
            return txMan.wrap {
                passcodeRepo.deleteByEmailAndPurpose(rootUserFromRequest.email, VerifyEmailRequest.Purpose.signup)
                // Create Organization
                organizationRepo.insert(
                    Organizations(
                        organizationId,
                        request.name,
                        request.description ?: "",
                        ResourceHrn(
                            organization = organizationId,
                            resource = IamResources.USER,
                            resourceInstance = username
                        ).toString(),
                        JSONB.jsonb(gson.toJson(identityGroup)),
                        logTimestamp,
                        logTimestamp
                    )
                )

                // Create root user for the organization
                val rootUser = usersService.createUser(
                    organizationId = organizationId,
                    username = username,
                    preferredUsername = rootUserFromRequest.preferredUsername,
                    name = rootUserFromRequest.name,
                    email = rootUserFromRequest.email,
                    phoneNumber = rootUserFromRequest.phone ?: "",
                    password = rootUserFromRequest.password,
                    createdBy = "iam-system",
                    verified = true,
                    loginAccess = true
                )

                val policyHrns = policyTemplatesService
                    .createPersistAndReturnRootPolicyRecordsForOrganization(organizationId, rootUser)
                    .map { ResourceHrn(it.hrn) }

                // TODO: Avoid this duplicate call by returning the created organization from `organizationRepo.insert`
                val organization = getOrganization(organizationId)
                val userHrn = hrnFactory.getHrn(rootUser.hrn)

                if (policyHrns.isNotEmpty()) {
                    principalPolicyService.attachPoliciesToUser(userHrn, policyHrns)
                }

                val token = tokenService.generateJwtToken(userHrn)
                return@wrap Pair(organization, token)
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception when creating organization. Rolling back..." }

            identityProvider.deleteIdentityGroup(identityGroup)
            throw e.cause ?: e
        }
    }

    override suspend fun createOauthOrganization(
        companyName: String,
        name: String,
        email: String
    ): Pair<Organization, TokenResponse> {
        val organizationId = idGenerator.organizationId()
        val username = idGenerator.username()
        val logTimestamp = LocalDateTime.now()

        @Suppress("TooGenericExceptionCaught")
        try {
            return txMan.wrap {
                // Create Organization
                organizationRepo.insert(
                    Organizations(
                        organizationId,
                        companyName,
                        "",
                        ResourceHrn(
                            organization = organizationId,
                            resource = IamResources.USER,
                            resourceInstance = username
                        ).toString(),
                        null,
                        logTimestamp,
                        logTimestamp
                    )
                )

                // Create root user for the organization
                val rootUser = usersService.createOauthUser(
                    organizationId = organizationId,
                    username = username,
                    preferredUsername = null,
                    name = name,
                    email = email,
                    createdBy = "iam-system",
                    verified = true,
                    loginAccess = true
                )

                val policyHrns = policyTemplatesService
                    .createPersistAndReturnRootPolicyRecordsForOrganization(organizationId, rootUser)
                    .map { ResourceHrn(it.hrn) }

                // TODO: Avoid this duplicate call by returning the created organization from `organizationRepo.insert`
                val organization = getOrganization(organizationId)
                val userHrn = hrnFactory.getHrn(rootUser.hrn)

                if (policyHrns.isNotEmpty()) {
                    principalPolicyService.attachPoliciesToUser(userHrn, policyHrns)
                }

                val token = tokenService.generateJwtToken(userHrn)

                if (AppConfig.configuration.postHook.signUp != null) {
                    val body = gson.toJson(mapOf("organization" to organization))
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val httpClient = OkHttpClient()
                    val requestBuilder = Request.Builder()
                        .url(AppConfig.configuration.postHook.signUp)
                        .method("POST", body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Connection", "keep-alive")
                    val request = requestBuilder.build()
                    val response = httpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        logger.error { "Post hook failed with status code ${response.code}" }
                        logger.error { "Post hook failed with response ${response.body?.string()} " }
                        throw InternalException("Post hook failed")
                    }
                }

                return@wrap Pair(organization, token)
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception when creating organization. Rolling back..." }
            throw e.cause ?: e
        }
    }

    @Timed("organization.get") // TODO: Make this work
    override suspend fun getOrganization(id: String): Organization {
        val response = organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        val rootUser = usersService.getUser(response.id, ResourceHrn(response.rootUserHrn).resourceInstance!!)
        return Organization(
            id = response.id,
            name = response.name,
            description = response.description,
            rootUser = rootUser,
            createdAt = response.createdAt.toUTCOffset(),
            updatedAt = response.updatedAt.toUTCOffset()
        )
    }

    override suspend fun updateOrganization(id: String, name: String?, description: String?): Organization {
        organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        val updatedOrgRecord =
            organizationRepo.update(id, name, description) ?: throw InternalException("Internal service failure")
        val rootUser = usersService.getUser(
            updatedOrgRecord.id,
            ResourceHrn(updatedOrgRecord.rootUserHrn).resourceInstance!!
        )
        return Organization(
            id = updatedOrgRecord.id,
            name = updatedOrgRecord.name,
            description = updatedOrgRecord.description,
            rootUser = rootUser,
            createdAt = updatedOrgRecord.createdAt.toUTCOffset(),
            updatedAt = updatedOrgRecord.updatedAt.toUTCOffset()
        )
    }

    override suspend fun deleteOrganization(id: String): BaseSuccessResponse {
        val org = organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        organizationRepo.deleteById(id)

        val identityGroup = gson.fromJson(org.metadata.data(), IdentityGroup::class.java)
        identityProvider.deleteIdentityGroup(identityGroup)

        return BaseSuccessResponse(true)
    }
}

/**
 * Service which holds logic related to Organization operations
 */
interface OrganizationsService {
    suspend fun createOrganization(
        request: CreateOrganizationRequest
    ): Pair<Organization, TokenResponse>

    suspend fun createOauthOrganization(
        companyName: String,
        name: String,
        email: String
    ): Pair<Organization, TokenResponse>

    suspend fun getOrganization(id: String): Organization
    suspend fun updateOrganization(id: String, name: String?, description: String?): Organization
    suspend fun deleteOrganization(id: String): BaseSuccessResponse
}

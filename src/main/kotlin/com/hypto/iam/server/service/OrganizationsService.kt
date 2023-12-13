package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.PrincipalPoliciesRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.records.OrganizationsRecord
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.extensions.toUTCOffset
import com.hypto.iam.server.idp.IdentityGroup
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
import org.koin.core.qualifier.named

class OrganizationAlreadyExistException(message: String) : Exception(message)

private val logger = KotlinLogging.logger { }

class OrganizationsServiceImpl : KoinComponent, OrganizationsService {
    private val usersService: UsersService by inject()
    private val tokenService: TokenService by inject()
    private val hrnFactory: HrnFactory by inject()
    private val principalPolicyService: PrincipalPolicyService by inject()
    private val policyTemplatesService: PolicyTemplatesService by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val gson: Gson by inject()
    private val txMan: TxMan by inject()
    private val httpClient: OkHttpClient by inject(named("AuthProvider"))
    private val appConfig: AppConfig by inject()
    private val credentialsRepo: CredentialsRepo by inject()
    private val principalPolicyRepo: PrincipalPoliciesRepo by inject()
    private val policyRepo: PoliciesRepo by inject()
    private val userAuthRepo: UserAuthRepo by inject()
    private val usersRepo: UserRepo by inject()
    private val passcodeRepo: PasscodeRepo by inject()
    private val organizationRepo: OrganizationRepo by inject()

    override suspend fun createOrganization(
        request: CreateOrganizationRequest,
        issuer: String
    ): Pair<Organization, TokenResponse> {
        val organizationId = idGenerator.organizationId()
        val identityGroup = appConfig.cognito
        val username = idGenerator.username()
        val logTimestamp = LocalDateTime.now()
        val rootUserFromRequest = request.rootUser

        @Suppress("TooGenericExceptionCaught")
        try {
            return txMan.wrap {
                passcodeRepo.deleteByEmailAndPurpose(rootUserFromRequest.email, VerifyEmailRequest.Purpose.signup)
                // Create Organization
                val organizationsRecord = OrganizationsRecord(
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
                organizationRepo.store(organizationsRecord)

                // Create root user for the organization
                val rootUser = usersService.createUser(
                    organizationId = organizationId,
                    subOrganizationId = null,
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

                val organization = Organization(
                    id = organizationsRecord.id,
                    name = organizationsRecord.name,
                    description = organizationsRecord.description,
                    rootUser = rootUser,
                    createdAt = organizationsRecord.createdAt.toUTCOffset(),
                    updatedAt = organizationsRecord.updatedAt.toUTCOffset()
                )
                val userHrn = hrnFactory.getHrn(rootUser.hrn)

                if (policyHrns.isNotEmpty()) {
                    principalPolicyService.attachPoliciesToUser(userHrn, policyHrns)
                }

                val token = tokenService.generateJwtToken(userHrn)
                executePostHook(organization)

                userAuthRepo.create(
                    hrn = userHrn.toString(),
                    providerName = issuer,
                    authMetadata = null
                )

                return@wrap Pair(organization, token)
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception when creating organization. Rolling back..." }

            throw e.cause ?: e
        }
    }

    override suspend fun createOauthOrganization(
        companyName: String,
        name: String,
        email: String,
        issuer: String
    ): Pair<Organization, TokenResponse> {
        val organizationId = idGenerator.organizationId()
        val username = idGenerator.username()
        val logTimestamp = LocalDateTime.now()

        @Suppress("TooGenericExceptionCaught")
        try {
            return txMan.wrap {
                // Create Organization
                val organizationsRecord = OrganizationsRecord(
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
                organizationRepo.store(organizationsRecord)

                // Create root user for the organization
                val rootUser = usersService.createUser(
                    organizationId = organizationId,
                    subOrganizationId = null,
                    username = username,
                    preferredUsername = null,
                    name = name,
                    email = email,
                    phoneNumber = null,
                    password = null,
                    createdBy = "iam-system",
                    verified = true,
                    loginAccess = true
                )

                val policyHrns = policyTemplatesService
                    .createPersistAndReturnRootPolicyRecordsForOrganization(organizationId, rootUser)
                    .map { ResourceHrn(it.hrn) }

                val organization = Organization(
                    id = organizationsRecord.id,
                    name = organizationsRecord.name,
                    description = organizationsRecord.description,
                    rootUser = rootUser,
                    createdAt = organizationsRecord.createdAt.toUTCOffset(),
                    updatedAt = organizationsRecord.updatedAt.toUTCOffset()
                )
                val userHrn = hrnFactory.getHrn(rootUser.hrn)

                if (policyHrns.isNotEmpty()) {
                    principalPolicyService.attachPoliciesToUser(userHrn, policyHrns)
                }

                val token = tokenService.generateJwtToken(userHrn)
                executePostHook(organization)
                userAuthRepo.create(
                    hrn = userHrn.toString(),
                    providerName = issuer,
                    authMetadata = null
                )

                return@wrap Pair(organization, token)
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception when creating organization. Rolling back..." }
            throw e.cause ?: e
        }
    }

    private fun executePostHook(organization: Organization) {
        val body = gson.toJson(mapOf("organization" to organization))
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val requestBuilder = Request.Builder()
            .url(AppConfig.configuration.postHook.signup)
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            logger.error { "Post hook failed with status code ${response.code}" }
            logger.error { "Post hook failed with response ${response.body?.string()} " }
            throw InternalException("Post hook failed")
        }
    }

    @Timed("organization.get") // TODO: Make this work
    override suspend fun getOrganization(id: String): Organization {
        val response = organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        val rootUser = usersService.getUser(response.id, null, ResourceHrn(response.rootUserHrn).resourceInstance!!)
        return Organization(
            id = response.id,
            name = response.name,
            description = response.description,
            rootUser = rootUser,
            createdAt = response.createdAt.toUTCOffset(),
            updatedAt = response.updatedAt.toUTCOffset()
        )
    }

    override suspend fun updateOrganization(
        id: String,
        name: String?,
        description: String?,
        identityGroup: IdentityGroup?
    ): Organization {
        organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")
        val updatedOrgRecord =
            organizationRepo.update(id, name, description, identityGroup) ?: throw InternalException(
                "Internal service failure"
            )
        val rootUser = usersService.getUser(
            updatedOrgRecord.id,
            null,
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
        organizationRepo.findById(id) ?: throw EntityNotFoundException("Organization id - $id not found")

        // TODO: Delete all resources associated with the organization on Cognito
        txMan.wrap {
            credentialsRepo.deleteByUserHrn(id)
            principalPolicyRepo.deleteByPrincipalHrn(id)
            policyRepo.deleteByOrganizationId(id)
            userAuthRepo.deleteByUserHrn(id)
            usersRepo.deleteByOrganizationId(id)
            passcodeRepo.deleteByOrganizationId(id)
            organizationRepo.deleteById(id)
        }

        return BaseSuccessResponse(true)
    }
}

/**
 * Service which holds logic related to Organization operations
 */
interface OrganizationsService {
    suspend fun createOrganization(
        request: CreateOrganizationRequest,
        issuer: String
    ): Pair<Organization, TokenResponse>

    suspend fun createOauthOrganization(
        companyName: String,
        name: String,
        email: String,
        issuer: String
    ): Pair<Organization, TokenResponse>

    suspend fun getOrganization(id: String): Organization
    suspend fun updateOrganization(
        id: String,
        name: String?,
        description: String?,
        identityGroup: IdentityGroup?
    ): Organization
    suspend fun deleteOrganization(id: String): BaseSuccessResponse
}

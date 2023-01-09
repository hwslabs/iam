package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.PasscodeLimitExceededException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Passcode
import com.hypto.iam.server.models.PasscodePaginatedResponse
import com.hypto.iam.server.models.VerifyEmailRequest.Purpose
import com.hypto.iam.server.security.AuthorizationException
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.EncryptUtil
import com.hypto.iam.server.validators.InviteMetadata
import java.time.LocalDateTime
import java.util.Base64
import mu.KotlinLogging
import org.apache.http.client.utils.URIBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest

data class ResetPasswordTemplateData(
    val link: String,
    val name: String
)

data class SignupTemplateData(
    val link: String
)

data class InviteUserTemplateData(
    val link: String,
    val nameOfUser: String,
    val organizationName: String,
)

private val logger = KotlinLogging.logger("service.TokenService")

class PasscodeServiceImpl : KoinComponent, PasscodeService {
    private val sesClient: SesClient by inject()
    private val appConfig: AppConfig by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val usersService: UsersService by inject()
    private val userRepo: UserRepo by inject()
    private val organizationRepo: OrganizationRepo by inject()
    private val policiesRepo: PoliciesRepo by inject()
    private val passcodeRepo: PasscodeRepo by inject()
    private val gson: Gson by inject()
    private val encryptUtil: EncryptUtil by inject()

    override suspend fun encryptMetadata(metadata: Map<String, Any>): String {
        val metadataJson = gson.toJson(metadata)
        return encryptUtil.encrypt(metadataJson)
    }

    override suspend fun decryptMetadata(metadata: String): Map<String, Any> {
        val metadataJson = encryptUtil.decrypt(metadata)
        return gson.fromJson(metadataJson, Map::class.java) as Map<String, Any>
    }

    override suspend fun verifyEmail(
        email: String,
        purpose: Purpose,
        organizationId: String?,
        metadata: Map<String, Any>?,
        principal: UserPrincipal?
    ): BaseSuccessResponse {
        // Validations
        if (passcodeRepo.getValidPasscodeCount(email, purpose) >= appConfig.app.passcodeCountLimit) {
            throw PasscodeLimitExceededException(
                "You can only send ${appConfig.app.passcodeCountLimit} passcodes per day"
            )
        }
        if (purpose == Purpose.invite) {
            val inviteMetadata = InviteMetadata(metadata!!)
            require(policiesRepo.existsByIds(inviteMetadata.policies.map { it })) {
                "Invalid policies found"
            }
        }
        if (purpose == Purpose.invite) {
            val oldInviteRecord = passcodeRepo.getValidPasscodeCount(email, Purpose.invite, organizationId!!)
            if (oldInviteRecord > 0) {
                passcodeRepo.deleteByEmailAndPurpose(email, purpose, organizationId)
            }
        }
        val validUntil = LocalDateTime.now().plusSeconds(appConfig.app.passcodeValiditySeconds)

        val passcodeRecord = PasscodesRecord().apply {
            this.id = idGenerator.passcodeId()
            this.email = email
            this.organizationId = if (purpose == Purpose.signup) null else organizationId
            this.validUntil = validUntil
            this.purpose = purpose.toString()
            this.createdAt = LocalDateTime.now()
            this.metadata = metadata?.let { encryptMetadata(it) }
        }
        val passcode = passcodeRepo.createPasscode(passcodeRecord)
        val response = when (purpose) {
            Purpose.signup -> sendSignupPasscode(email, passcode.id)
            Purpose.reset -> sendResetPassword(email, organizationId, passcode.id)
            Purpose.invite -> sendInviteUserPasscode(
                email,
                organizationId!!,
                passcode.id,
                principal ?: throw AuthorizationException("User is not authorized")
            )
        }
        return BaseSuccessResponse(response)
    }

    private fun createPasscodeLink(
        passcode: String,
        email: String,
        purpose: Purpose,
        organizationId: String? = null
    ): String {
        val link = URIBuilder()
            .setScheme("https")
            .setHost(appConfig.app.baseUrl)

        link.path = when (purpose) {
            Purpose.signup -> "/signup"
            Purpose.reset -> "/organizations/${organizationId!!}/users/resetPassword"
            Purpose.invite -> "/organizations/${organizationId!!}/users/verifyUser"
        }

        return link.setParameter("passcode", passcode)
            .setParameter("email", Base64.getEncoder().encodeToString(email.toByteArray()))
            .build()
            .toString()
    }

    private fun sendSignupPasscode(email: String, passcode: String): Boolean {
        val link = createPasscodeLink(passcode, email, Purpose.signup)
        val templateData = SignupTemplateData(link)
        val emailRequest = SendTemplatedEmailRequest.builder()
            .source(appConfig.app.senderEmailAddress)
            .template(appConfig.app.signUpEmailTemplate)
            .templateData(gson.toJson(templateData))
            .destination(Destination.builder().toAddresses(email).build())
            .build()
        sesClient.sendTemplatedEmail(emailRequest)
        return true
    }

    private suspend fun sendResetPassword(email: String, organizationId: String?, passcode: String): Boolean {
        val user = usersService.getUserByEmail(organizationId, email)
        if (!user.loginAccess) {
            throw AuthorizationException("User does not have login access")
        }
        val link = createPasscodeLink(passcode, email, Purpose.reset, user.organizationId)
        val templateData = ResetPasswordTemplateData(link, user.name)
        val emailRequest = SendTemplatedEmailRequest.builder()
            .source(appConfig.app.senderEmailAddress)
            .template(appConfig.app.resetPasswordEmailTemplate)
            .templateData(gson.toJson(templateData))
            .destination(Destination.builder().toAddresses(user.email).build())
            .build()
        sesClient.sendTemplatedEmail(emailRequest)
        return true
    }

    private suspend fun sendInviteUserPasscode(
        email: String,
        orgId: String,
        passcode: String,
        principal: UserPrincipal
    ): Boolean {
        try {
            usersService.getUserByEmail(null, email)
            throw EntityAlreadyExistsException("User with email $email already exists")
        } catch (e: EntityNotFoundException) {
            logger.info { "User with email $email does not exist" }
        }
        val link = createPasscodeLink(passcode, email, Purpose.invite, orgId)

        val user = userRepo.findByHrn(principal.hrnStr) ?: throw EntityNotFoundException("User not found")
        if (!user.loginAccess) {
            throw AuthorizationException("User does not have login access")
        }
        val organization =
            organizationRepo.findById(orgId) ?: throw EntityNotFoundException("Organization id - $orgId not found")
        val nameOfUser = user.name ?: user.preferredUsername ?: user.email
        val templateData = InviteUserTemplateData(link, nameOfUser, organization.name)
        val emailRequest = SendTemplatedEmailRequest.builder()
            .source(appConfig.app.senderEmailAddress)
            .template(appConfig.app.inviteUserEmailTemplate)
            .templateData(gson.toJson(templateData))
            .destination(Destination.builder().toAddresses(email).build())
            .build()
        sesClient.sendTemplatedEmail(emailRequest)
        return true
    }

    override suspend fun listOrgPasscodes(
        organizationId: String,
        purpose: Purpose,
        paginationContext: PaginationContext
    ): PasscodePaginatedResponse {
        val passcodes = passcodeRepo.listPasscodes(organizationId, purpose, paginationContext)

        val newContext = PaginationContext.from(passcodes.lastOrNull()?.id, paginationContext)
        return PasscodePaginatedResponse(
            passcodes.map { Passcode.from(it) },
            newContext.nextToken,
            newContext.toOptions()
        )
    }
}

interface PasscodeService {
    suspend fun verifyEmail(
        email: String,
        purpose: Purpose,
        organizationId: String?,
        metadata: Map<String, Any>?,
        principal: UserPrincipal?
    ): BaseSuccessResponse

    suspend fun listOrgPasscodes(
        organizationId: String,
        purpose: Purpose,
        paginationContext: PaginationContext
    ): PasscodePaginatedResponse

    suspend fun encryptMetadata(metadata: Map<String, Any>): String
    suspend fun decryptMetadata(metadata: String): Map<String, Any>
}

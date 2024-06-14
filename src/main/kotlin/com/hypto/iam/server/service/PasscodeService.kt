package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.SubOrganizationRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.exceptions.DbExceptionHandler
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
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.validators.InviteMetadata
import com.txman.TxMan
import io.ktor.server.plugins.BadRequestException
import io.ktor.util.logging.error
import mu.KotlinLogging
import org.apache.http.client.utils.URIBuilder
import org.jooq.exception.DataAccessException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SesException
import java.time.LocalDateTime
import java.util.Base64

data class ResetPasswordTemplateData(
    val link: String,
    val name: String,
)

data class SignupTemplateData(
    val link: String,
)

data class InviteUserTemplateData(
    val link: String,
    val nameOfUser: String,
    val organizationName: String,
    val subOrganizationName: String?,
)

data class RequestAccessTemplateData(
    val link: String,
    val nameOfUser: String,
    val organizationName: String,
    val subOrganizationName: String?,
)

const val USER_NOT_FOUND_EXCEPTION_MESSAGE = "User not found"

private val logger = KotlinLogging.logger("service.TokenService")

class PasscodeServiceImpl : KoinComponent, PasscodeService {
    private val sesClient: SesClient by inject()
    private val appConfig: AppConfig by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val usersService: UsersService by inject()
    private val userRepo: UserRepo by inject()
    private val organizationRepo: OrganizationRepo by inject()
    private val subOrganizationRepo: SubOrganizationRepo by inject()
    private val policiesRepo: PoliciesRepo by inject()
    private val passcodeRepo: PasscodeRepo by inject()
    private val gson: Gson by inject()
    private val encryptUtil: EncryptUtil by inject()
    private val txMan: TxMan by inject()

    override suspend fun encryptMetadata(metadata: Map<String, Any>): String {
        val metadataJson = gson.toJson(metadata)
        return encryptUtil.encrypt(metadataJson)
    }

    override suspend fun decryptMetadata(metadata: String): Map<String, Any> {
        val metadataJson = encryptUtil.decrypt(metadata)
        return gson.fromJson(metadataJson, Map::class.java) as Map<String, Any>
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun verifyEmail(
        email: String,
        userHrn: String?,
        purpose: Purpose,
        organizationId: String?,
        subOrganizationName: String?,
        metadata: Map<String, Any>?,
        principal: UserPrincipal?,
    ): BaseSuccessResponse {
        // Validations
        if (passcodeRepo.getValidPasscodeCount(
                email,
                purpose,
                organizationId,
                subOrganizationName,
            ) >= appConfig.app.passcodeCountLimit
        ) {
            throw PasscodeLimitExceededException(
                "You can only send ${appConfig.app.passcodeCountLimit} passcodes per day",
            )
        }
        if (purpose == Purpose.invite) {
            val inviteMetadata = InviteMetadata(metadata!!)

            // Validate policies to be attached to invited user
            require(policiesRepo.existsByIds(inviteMetadata.policies.map { it })) {
                "Invalid policies found"
            }

            // Clean-up old invites
            val oldInviteRecord =
                passcodeRepo.getValidPasscodeCount(
                    email,
                    Purpose.invite,
                    organizationId!!,
                    subOrganizationName,
                )
            if (oldInviteRecord > 0) {
                passcodeRepo.deleteByEmailAndPurpose(email, purpose, organizationId)
            }
        }

        if (purpose == Purpose.link_user) {
            // Clean-up old invites
            val oldInviteRecord =
                passcodeRepo.getValidPasscodeCount(
                    email,
                    Purpose.link_user,
                    organizationId!!,
                    subOrganizationName,
                )
            if (oldInviteRecord > 0) {
                passcodeRepo.deleteByEmailAndPurpose(email, purpose, organizationId)
            }
        }

        val validUntil = LocalDateTime.now().plusSeconds(appConfig.app.passcodeValiditySeconds)
        val response =
            try {
                txMan.wrap {
                    val passcodeRecord =
                        PasscodesRecord().apply {
                            this.id = idGenerator.passcodeId()
                            this.email = email
                            this.organizationId = if (purpose == Purpose.signup) null else organizationId
                            this.subOrganizationName = subOrganizationName
                            this.validUntil = validUntil
                            this.lastSent = LocalDateTime.now()
                            this.purpose = purpose.toString()
                            this.createdAt = LocalDateTime.now()
                            this.metadata = metadata?.let { encryptMetadata(it) }
                        }
                    val passcode = passcodeRepo.createPasscode(passcodeRecord)
                    when (purpose) {
                        Purpose.signup -> sendSignupPasscode(email, passcode.id)
                        Purpose.reset -> sendResetPassword(email, organizationId, subOrganizationName, passcode.id)
                        Purpose.invite ->
                            sendInviteUserPasscode(
                                email,
                                userHrn,
                                organizationId!!,
                                subOrganizationName,
                                passcode.id,
                                principal ?: throw AuthorizationException("User is not authorized"),
                            )
                        Purpose.link_user ->
                            sendRequestAccessPasscode(
                                email,
                                userHrn,
                                organizationId!!,
                                subOrganizationName,
                                passcode.id,
                                principal ?: throw AuthorizationException("User is not authorized"),
                            )
                    }
                }
            } catch (e: DataAccessException) {
                logger.error { "Error occurred while creating passcode record: $e" }
                throw DbExceptionHandler.mapToApplicationException(e)
            }
        return BaseSuccessResponse(response)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createPasscodeLink(
        passcode: String,
        email: String,
        purpose: Purpose,
        userHrn: String? = null,
        organizationId: String? = null,
        subOrganizationName: String? = null,
    ): String {
        val baseUrl =
            if (subOrganizationName.isNullOrEmpty()) {
                appConfig.app.baseUrl
            } else {
                appConfig.subOrgConfig.baseUrl
            }
        val link =
            URIBuilder()
                .setScheme("https")
                .setHost(baseUrl)

        link.path =
            when (subOrganizationName) {
                null -> {
                    when (purpose) {
                        Purpose.signup -> appConfig.onboardRoutes.signup
                        Purpose.reset -> appConfig.onboardRoutes.reset
                        Purpose.invite -> appConfig.onboardRoutes.invite
                        Purpose.link_user -> appConfig.onboardRoutes.requestAccess
                    }
                }
                else -> {
                    when (purpose) {
                        Purpose.signup -> appConfig.subOrgConfig.onboardRoutes.signup
                        Purpose.reset -> appConfig.subOrgConfig.onboardRoutes.reset
                        Purpose.invite -> appConfig.subOrgConfig.onboardRoutes.invite
                        Purpose.link_user -> appConfig.subOrgConfig.onboardRoutes.requestAccess
                    }
                }
            }

        link.setParameter("passcode", passcode)
        link.setParameter("email", Base64.getEncoder().encodeToString(email.toByteArray()))
        userHrn?.let {
            link.setParameter("userHrn", it)
        }

        organizationId?.let {
            link.setParameter("organizationId", it)
        }

        subOrganizationName?.let {
            link.setParameter("subOrganizationName", it)
        }

        link.setParameter("purpose", purpose.toString())
        return link.build().toString()
    }

    private fun sendSignupPasscode(
        email: String,
        passcode: String,
    ): Boolean {
        val link = createPasscodeLink(passcode = passcode, email = email, purpose = Purpose.signup)
        val templateData = SignupTemplateData(link)
        val emailRequest =
            SendTemplatedEmailRequest.builder()
                .source(appConfig.app.senderEmailAddress)
                .template(appConfig.app.signUpEmailTemplate)
                .templateData(gson.toJson(templateData))
                .destination(Destination.builder().toAddresses(email).build())
                .build()
        try {
            sesClient.sendTemplatedEmail(emailRequest)
        } catch (e: SesException) {
            val exceptionMessage: String? = e.message
            if (exceptionMessage != null && exceptionMessage.contains("Domain contains illegal character")) {
                logger.error("Email contains illegal characters")
                throw BadRequestException("Invalid email address: $email")
            } else {
                logger.error(e)
                throw e
            }
        }
        return true
    }

    private suspend fun sendResetPassword(
        email: String,
        organizationId: String?,
        subOrganizationId: String?,
        passcode: String,
    ): Boolean {
        val user = usersService.getUserByEmail(organizationId, subOrganizationId, email)
        if (!user.loginAccess) {
            throw AuthorizationException("User does not have login access")
        }
        val templateName =
            if (subOrganizationId.isNullOrEmpty()) {
                appConfig.app.resetPasswordEmailTemplate
            } else {
                appConfig.subOrgConfig.resetPasswordEmailTemplate
            }
        val link = createPasscodeLink(passcode = passcode, email = email, purpose = Purpose.reset, organizationId = user.organizationId)
        val templateData = ResetPasswordTemplateData(link, user.name)
        val emailRequest =
            SendTemplatedEmailRequest.builder()
                .source(appConfig.app.senderEmailAddress)
                .template(templateName)
                .templateData(gson.toJson(templateData))
                .destination(Destination.builder().toAddresses(user.email).build())
                .build()
        sesClient.sendTemplatedEmail(emailRequest)
        return true
    }

    @Suppress("ThrowsCount")
    private suspend fun sendInviteUserPasscode(
        email: String,
        userHrn: String?,
        orgId: String,
        subOrganizationName: String?,
        passcode: String,
        principal: UserPrincipal,
    ): Boolean {
        var templateName: String? = null
        try {
            if (!subOrganizationName.isNullOrEmpty()) {
                templateName = appConfig.subOrgConfig.inviteUserEmailTemplate
                val user = usersService.getUserByEmail(orgId, subOrganizationName, email)
                require(!user.loginAccess) {
                    "User with email $email already has login access in sub-org $subOrganizationName"
                }
            } else {
                templateName = appConfig.app.inviteUserEmailTemplate
                val user = usersService.getUserByEmail(orgId, null, email)
                require(!user.loginAccess) {
                    "User with email $email already has login access"
                }
            }
        } catch (e: EntityNotFoundException) {
            logger.info { "User with email $email does not exist" }
        }
        val link = createPasscodeLink(passcode = passcode, email = email, purpose = Purpose.invite, userHrn = userHrn, organizationId = orgId, subOrganizationName = subOrganizationName)
        val user = userRepo.findByHrn(principal.hrnStr) ?: throw EntityNotFoundException(USER_NOT_FOUND_EXCEPTION_MESSAGE)
        if (!user.loginAccess) {
            throw AuthorizationException("User token does not have login access")
        }
        val organization =
            organizationRepo.findById(orgId) ?: throw EntityNotFoundException("Organization id - $orgId not found")
        subOrganizationName?.let {
            subOrganizationRepo.fetchById(orgId, it)
                ?: throw EntityNotFoundException("Sub organization id - $subOrganizationName not found")
        }

        val nameOfUser = user.name ?: user.preferredUsername ?: user.email
        val templateData = InviteUserTemplateData(link, nameOfUser, organization.name, subOrganizationName)
        val emailRequest =
            SendTemplatedEmailRequest.builder()
                .source(appConfig.app.senderEmailAddress)
                .template(templateName)
                .templateData(gson.toJson(templateData))
                .destination(Destination.builder().toAddresses(email).build())
                .build()
        val response = sesClient.sendTemplatedEmail(emailRequest)
        logger.info { "Email sent to $email with message id ${response.messageId()}" }
        return true
    }

    override suspend fun resendInvitePasscode(
        orgId: String,
        subOrganizationName: String?,
        email: String,
        principal: UserPrincipal,
    ): Boolean {
        val record =
            passcodeRepo.getValidPasscodeByEmail(
                organizationId = orgId,
                subOrganizationName = subOrganizationName,
                purpose = Purpose.invite,
                email = email,
            ) ?: throw EntityNotFoundException("No invite email found for $email")
        require(record.lastSent.plusSeconds(appConfig.app.resendInviteWaitTimeSeconds) < LocalDateTime.now()) {
            "Resend invite email after some time"
        }
        val link = createPasscodeLink(passcode = record.id, email = email, purpose = Purpose.invite, organizationId = orgId)

        val invitingUser = userRepo.findByHrn(principal.hrnStr) ?: throw EntityNotFoundException(USER_NOT_FOUND_EXCEPTION_MESSAGE)
        if (!invitingUser.loginAccess) {
            throw AuthorizationException("User does not have login access")
        }
        val organization =
            organizationRepo.findById(orgId) ?: throw EntityNotFoundException("Organization id - $orgId not found")
        val nameOfUser = invitingUser.name ?: invitingUser.preferredUsername ?: invitingUser.email
        val templateData = InviteUserTemplateData(link, nameOfUser, organization.name, subOrganizationName)
        val emailRequest =
            SendTemplatedEmailRequest.builder()
                .source(appConfig.app.senderEmailAddress)
                .template(appConfig.app.inviteUserEmailTemplate)
                .templateData(gson.toJson(templateData))
                .destination(Destination.builder().toAddresses(email).build())
                .build()
        val response = sesClient.sendTemplatedEmail(emailRequest)
        logger.info { "Resent invite email to $email with message id ${response.messageId()}" }
        passcodeRepo.updateLastSent(record.id, LocalDateTime.now())
        return true
    }

    @Suppress("ThrowsCount")
    private suspend fun sendRequestAccessPasscode(
        email: String,
        userHrn: String?,
        orgId: String,
        subOrganizationName: String?,
        passcode: String,
        principal: UserPrincipal,
    ): Boolean {
        val inviteeUser =
            userHrn?.let { usersService.getUser(ResourceHrn(it)) }
                ?: usersService.getUserByEmail(null, null, email)
        require(inviteeUser.loginAccess) {
            "Requesting access of another user can be done only if the invitee has login access"
        }
        val user = userRepo.findByHrn(principal.hrnStr) ?: throw EntityNotFoundException(USER_NOT_FOUND_EXCEPTION_MESSAGE)
        if (!user.loginAccess) {
            throw AuthorizationException("User token does not have login access")
        }
        val organization =
            organizationRepo.findById(orgId) ?: throw EntityNotFoundException("Organization id - $orgId not found")
        subOrganizationName?.let {
            subOrganizationRepo.fetchById(orgId, it)
                ?: throw EntityNotFoundException("Sub organization id - $subOrganizationName not found")
        }

        val nameOfUser = user.name ?: user.preferredUsername ?: user.email
        val link = createPasscodeLink(passcode = passcode, email = email, purpose = Purpose.link_user)
        val templateData = RequestAccessTemplateData(link, nameOfUser, organization.name, subOrganizationName)
        val templateName =
            if (subOrganizationName.isNullOrEmpty()) {
                appConfig.app.requestAccessEmailTemplate
            } else {
                appConfig.subOrgConfig.requestAccessEmailTemplate
            }
        val emailRequest =
            SendTemplatedEmailRequest.builder()
                .source(appConfig.app.senderEmailAddress)
                .template(templateName)
                .templateData(gson.toJson(templateData))
                .destination(Destination.builder().toAddresses(email).build())
                .build()
        val response = sesClient.sendTemplatedEmail(emailRequest)
        logger.info { "Email sent to $email with message id ${response.messageId()}" }
        return true
    }

    override suspend fun listOrgPasscodes(
        organizationId: String,
        subOrganizationName: String?,
        purpose: Purpose,
        paginationContext: PaginationContext,
    ): PasscodePaginatedResponse {
        val passcodes = passcodeRepo.listPasscodes(organizationId, subOrganizationName, purpose, paginationContext)

        val newContext = PaginationContext.from(passcodes.lastOrNull()?.id, paginationContext)
        return PasscodePaginatedResponse(
            passcodes.map { Passcode.from(it) },
            newContext.nextToken,
            newContext.toOptions(),
        )
    }
}

interface PasscodeService {
    suspend fun verifyEmail(
        email: String,
        userHrn: String?,
        purpose: Purpose,
        organizationId: String?,
        subOrganizationName: String?,
        metadata: Map<String, Any>?,
        principal: UserPrincipal?,
    ): BaseSuccessResponse

    suspend fun listOrgPasscodes(
        organizationId: String,
        subOrganizationName: String?,
        purpose: Purpose,
        paginationContext: PaginationContext,
    ): PasscodePaginatedResponse

    suspend fun resendInvitePasscode(
        orgId: String,
        subOrganizationName: String?,
        email: String,
        principal: UserPrincipal,
    ): Boolean

    suspend fun encryptMetadata(metadata: Map<String, Any>): String

    suspend fun decryptMetadata(metadata: String): Map<String, Any>
}

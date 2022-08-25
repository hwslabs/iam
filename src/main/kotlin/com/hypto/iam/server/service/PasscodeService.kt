package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.exceptions.PasscodeLimitExceededException
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.VerifyEmailRequest.Purpose
import com.hypto.iam.server.utils.ApplicationIdUtil
import java.time.LocalDateTime
import java.util.Base64
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

class PasscodeServiceImpl : KoinComponent, PasscodeService {
    private val sesClient: SesClient by inject()
    private val appConfig: AppConfig by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val usersService: UsersService by inject()
    private val passcodeRepo: PasscodeRepo by inject()
    private val gson: Gson by inject()

    override suspend fun verifyEmail(
        email: String,
        purpose: Purpose,
        organizationId: String?
    ): BaseSuccessResponse {
        if (passcodeRepo.getValidPasscodeCount(email, purpose) >= appConfig.app.passcodeCountLimit) {
            throw PasscodeLimitExceededException(
                "You can only send ${appConfig.app.passcodeCountLimit} passcodes per day"
            )
        }
        val validUntil = LocalDateTime.now().plusSeconds(appConfig.app.passcodeValiditySeconds)
        val passcode = passcodeRepo.createPasscode(
            idGenerator.passcodeId(),
            email,
            if (purpose == Purpose.signup) null else organizationId,
            validUntil,
            purpose
        )
        val response = when (purpose) {
            Purpose.signup -> sendSignupPasscode(email, passcode.id)
            Purpose.reset -> sendResetPassword(email, organizationId, passcode.id)
        }
        return BaseSuccessResponse(response)
    }

    private fun sendSignupPasscode(email: String, passcode: String): Boolean {
        val link = "${appConfig.app.baseUrl}/signup?passcode=$passcode&" +
            "email=${Base64.getEncoder().encodeToString(email.toByteArray())}"
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
        val link =
            "${appConfig.app.baseUrl}/organizations/${user.organizationId}/users/resetPassword?" +
                "passcode=$passcode&email=${Base64.getEncoder().encodeToString(email.toByteArray())}"
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
}

interface PasscodeService {
    suspend fun verifyEmail(
        email: String,
        purpose: Purpose,
        organizationId: String?
    ): BaseSuccessResponse
}

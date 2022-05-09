package com.hypto.iam.server.service

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.utils.ApplicationIdUtil
import java.time.LocalDateTime
import java.util.Base64
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest

class PasscodeServiceImpl : KoinComponent, PasscodeService {
    private val sesClient: SesClient by inject()
    private val appConfig: AppConfig by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()
    private val passcodeRepo: PasscodeRepo by inject()

    override suspend fun verifyEmail(email: String): BaseSuccessResponse {
        val validUntil = LocalDateTime.now().plusSeconds(appConfig.app.passcodeValiditySeconds)
        val passcode = passcodeRepo.createPasscode(
            idGenerator.passcodeId(),
            email,
            organizationId = null,
            validUntil,
            VerifyEmailRequest.Purpose.verify
        )
        val link = "${appConfig.app.baseUrl}/verify?passcode=${passcode.id}&email=${
            Base64.getEncoder().encodeToString(email.toByteArray())
        }"
        val emailRequest = SendTemplatedEmailRequest.builder()
            .source(appConfig.app.senderEmailAddress).template(appConfig.app.verifyUserTemplate).templateData(
                "{\"link\":\"$link\"}"
            ).destination(Destination.builder().toAddresses(email).build()).build()

        sesClient.sendTemplatedEmail(emailRequest)
        return BaseSuccessResponse(true)
    }
}

interface PasscodeService {
    suspend fun verifyEmail(email: String): BaseSuccessResponse
}

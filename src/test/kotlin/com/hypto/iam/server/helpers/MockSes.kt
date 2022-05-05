package com.hypto.iam.server.helpers

import io.mockk.coEvery
import org.koin.test.KoinTest
import org.koin.test.mock.declareMock
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse

fun KoinTest.mockSesClient(): SesClient {
    return declareMock {
        coEvery {
            this@declareMock.sendTemplatedEmail(any<SendTemplatedEmailRequest>())
        } coAnswers {
            SendTemplatedEmailResponse.builder().messageId("1234-5678-3421").build()
        }
    }
}

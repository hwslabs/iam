package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PasscodeApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `test verifyEmail api - success`() {
        withTestApplication(Application::handleRequest) {
            val requestBody = VerifyEmailRequest(
                email = "abcd@abcd.com",
                purpose = VerifyEmailRequest.Purpose.signup
            )
            with(
                handleRequest(
                    HttpMethod.Post,
                    "/verifyEmail"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(requestBody))
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                val responseBody =
                    gson.fromJson(response.content, BaseSuccessResponse::class.java)
                assertEquals(true, responseBody.success)
            }
        }
    }
}

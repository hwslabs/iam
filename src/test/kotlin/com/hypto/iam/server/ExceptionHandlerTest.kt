package com.hypto.iam.server

import com.google.gson.Gson
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.mock.declareMock

class ExceptionHandlerTest : AbstractContainerBaseTest() {
    private val gson = Gson()
    @Test
    fun `StatusPage - Respond to unhandled exceptions with statusCode_500 and custom error message`() {
        declareMock<OrganizationsService> {
            coEvery { this@declareMock.createOrganization(any(), any(), any()) } coAnswers {
                // Some exception which is not handled by Status Pages
                throw NumberFormatException()
            }
        }

        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val requestBody = CreateOrganizationRequest(
                orgName,
                AdminUser(userName, testPassword, testEmail, testPhone)
            )
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            ) {
                Assertions.assertEquals("{\"message\":\"Unknown Error Occurred\"}", response.content)
                Assertions.assertEquals(500, response.status()?.value)
            }
        }
    }
}

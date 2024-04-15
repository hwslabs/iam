package com.hypto.iam.server

import com.google.gson.Gson
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.RootUser
import com.hypto.iam.server.service.OrganizationsService
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.inject
import org.koin.test.mock.declareMock

class ExceptionHandlerTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `StatusPage - Respond to server side errors with custom error message`() {
        declareMock<OrganizationsService> {
            coEvery { this@declareMock.createOrganization(any(), TokenServiceImpl.ISSUER) } coAnswers {
                @Suppress("TooGenericExceptionThrown")
                throw RuntimeException()
            }
        }

        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val requestBody =
                CreateOrganizationRequest(
                    orgName,
                    RootUser(
                        preferredUsername = preferredUsername,
                        name = name,
                        password = testPassword,
                        email = testEmail,
                        phone = testPhone,
                    ),
                )
            val response =
                client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            Assertions.assertEquals("{\"message\":\"Internal Server Error Occurred\"}", response.bodyAsText())
            Assertions.assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }
}

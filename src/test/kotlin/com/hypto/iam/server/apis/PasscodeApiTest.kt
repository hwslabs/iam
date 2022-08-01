package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.idp.CognitoConstants
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
import io.mockk.coEvery
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.inject
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType

internal class PasscodeApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `test verifyEmail api for signup purpose - success`() {
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

    @Test
    fun `test verifyEmail api with orgId for signup purpose - success`() {
        withTestApplication(Application::handleRequest) {
            val requestBody = VerifyEmailRequest(
                email = "abcd@abcd.com",
                purpose = VerifyEmailRequest.Purpose.signup,
                organizationId = "sampleOrg"
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

    @Test
    fun `test verifyEmail api for reset purpose- success`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, createdUser) = DataSetupHelper.createOrganization(this)

            val listUsersResponse =
                ListUsersResponse.builder().users(
                    listOf(
                        UserType.builder().username(createdUser.username).enabled(true).attributes(
                            listOf(
                                AttributeType.builder().name(CognitoConstants.ATTRIBUTE_NAME)
                                    .value("test name")
                                    .build(),
                                AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL).value(createdUser.email)
                                    .build(),
                                AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PHONE).value(createdUser.phone)
                                    .build(),
                                AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL_VERIFIED).value("true")
                                    .build(),
                                AttributeType.builder().name(
                                    CognitoConstants.ATTRIBUTE_PREFIX_CUSTOM + CognitoConstants.ATTRIBUTE_CREATED_BY
                                ).value("iam-system").build()
                            )
                        ).userCreateDate(Instant.now()).build()
                    )
                ).build()
            coEvery {
                cognitoClient.listUsers(any<ListUsersRequest>())
            } returns listUsersResponse

            val organizationId = organizationResponse.organization!!.id

            val requestBody = VerifyEmailRequest(
                email = createdUser.email,
                purpose = VerifyEmailRequest.Purpose.reset,
                organizationId = organizationId
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

            DataSetupHelper.deleteOrganization(organizationId, this)
        }
    }

    @Test
    fun `send reset password for unknown email - failure`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organizationId = organizationResponse.organization!!.id

            val requestBody = VerifyEmailRequest(
                email = "unknownuser@email.com",
                purpose = VerifyEmailRequest.Purpose.reset,
                organizationId = organizationId
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
                assertEquals(HttpStatusCode.NotFound, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
            }

            DataSetupHelper.deleteOrganization(organizationId, this)
        }
    }
}

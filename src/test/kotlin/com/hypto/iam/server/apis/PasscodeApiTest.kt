package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV2.deleteOrganization
import com.hypto.iam.server.idp.CognitoConstants
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.ResendInviteRequest
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import java.time.Instant
import java.time.LocalDateTime
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
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val requestBody = VerifyEmailRequest(
                email = "abcd@abcd.com",
                purpose = VerifyEmailRequest.Purpose.signup
            )
            val response = client.post(
                "/verifyEmail"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(requestBody))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )
            val responseBody =
                gson.fromJson(response.bodyAsText(), BaseSuccessResponse::class.java)
            assertEquals(true, responseBody.success)
        }
    }

    @Test
    fun `test verifyEmail api with orgId for signup purpose - success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val requestBody = VerifyEmailRequest(
                email = "abcd@abcd.com",
                purpose = VerifyEmailRequest.Purpose.signup,
                organizationId = "sampleOrg"
            )
            val response = client.post(
                "/verifyEmail"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(requestBody))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )
            val responseBody =
                gson.fromJson(response.bodyAsText(), BaseSuccessResponse::class.java)
            assertEquals(true, responseBody.success)
        }
    }

    @Test
    fun `test verifyEmail api for reset purpose- success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, createdUser) = createOrganization()

            val listUsersResponse =
                ListUsersResponse.builder().users(
                    listOf(
                        UserType.builder().username(organizationResponse.organization.rootUser.username)
                            .enabled(true).attributes(
                                listOf(
                                    AttributeType.builder().name(CognitoConstants.ATTRIBUTE_NAME)
                                        .value("test name")
                                        .build(),
                                    AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                        .value(createdUser.email)
                                        .build(),
                                    AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PHONE)
                                        .value(createdUser.phone)
                                        .build(),
                                    AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL_VERIFIED)
                                        .value("true")
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

            val organizationId = organizationResponse.organization.id

            val requestBody = VerifyEmailRequest(
                email = createdUser.email,
                purpose = VerifyEmailRequest.Purpose.reset,
                organizationId = organizationId
            )
            val response = client.post(
                "/verifyEmail"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(requestBody))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )
            val responseBody =
                gson.fromJson(response.bodyAsText(), BaseSuccessResponse::class.java)
            assertEquals(true, responseBody.success)

            deleteOrganization(organizationId)
        }
    }

    @Test
    fun `send reset password for unknown email - failure`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organizationId = organizationResponse.organization!!.id

            val requestBody = VerifyEmailRequest(
                email = "unknownuser@email.com",
                purpose = VerifyEmailRequest.Purpose.reset,
                organizationId = organizationId
            )

            val response = client.post(
                "/verifyEmail"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(requestBody))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )

            deleteOrganization(organizationId,)
        }
    }

    @Test
    fun `missing admin user details in metadata while creating passcode for signup purpose - failure`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val metadata = mapOf<String, Any>(
                "name" to "test-org" + IdGenerator.randomId(),
                "rootUserName" to "test-name" + IdGenerator.randomId(),
                "rootUserVerified" to true,
                "rootUserPreferredUsername" to "user" + IdGenerator.randomId()
            )

            val verifyRequestBody = VerifyEmailRequest(
                email = testEmail,
                purpose = VerifyEmailRequest.Purpose.signup,
                metadata = metadata
            )
            val response = client.post("/verifyEmail") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(verifyRequestBody))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )
        }
    }

    @Test
    fun `resend invite for already invited user - success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organizationId = organizationResponse.organization.id
            val rootUserToken = organizationResponse.rootUserToken

            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val requestBody = VerifyEmailRequest(
                email = testEmail,
                purpose = VerifyEmailRequest.Purpose.invite,
                organizationId = organizationId,
                metadata = mapOf(
                    "inviterUserHrn" to organizationResponse.organization.rootUser.hrn,
                    "policies" to listOf("hrn:$organizationId::iam-policy/admin")
                )
            )

            coEvery {
                passcodeRepo.getValidPasscodeCount(any(), VerifyEmailRequest.Purpose.invite, any())
            } coAnswers {
                0
            }
            val response = client.post("/verifyEmail") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(requestBody))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )

            coEvery {
                passcodeRepo.getValidPasscodeByEmail(
                    organizationId,
                    VerifyEmailRequest.Purpose.invite,
                    testEmail
                )
            } coAnswers {
                PasscodesRecord(
                    "test-id",
                    LocalDateTime.now(),
                    testEmail,
                    organizationId,
                    VerifyEmailRequest.Purpose.invite.value,
                    LocalDateTime.now(),
                    null
                )
            }
            val resendEmailResponse = client.post("/organizations/$organizationId/invites/resend") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(ResendInviteRequest(testEmail)))
            }
            assertEquals(HttpStatusCode.OK, resendEmailResponse.status)
        }
    }

    @Test
    fun `resend invite for non invited user - failure`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, createdUser) = createOrganization()
            val rootUserToken = organizationResponse.rootUserToken
            val organizationId = organizationResponse.organization.id
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"

            coEvery {
                passcodeRepo.getValidPasscodeByEmail(
                    organizationId = organizationId,
                    purpose = VerifyEmailRequest.Purpose.invite,
                    email = testEmail
                )
            } coAnswers {
                null
            }

            val resendEmailResponse = client.post(
                "/organizations/$organizationId/invites/resend"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(ResendInviteRequest(testEmail)))
            }
            assertEquals(HttpStatusCode.NotFound, resendEmailResponse.status)
        }
    }
}

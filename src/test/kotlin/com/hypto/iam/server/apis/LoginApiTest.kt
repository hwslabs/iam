package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.TokenResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.util.Base64
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.test.mock.declareMock
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*

class LoginApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `Login with Uniqueness flag as false`() {
        withTestApplication(Application::handleRequest) {
            val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

            val authString = "${createdUser.email}:${createdUser.passwordHash}"
            val authHeader = "Basic ${Base64.getEncoder().encode(authString.encodeToByteArray())}"

            declareMock<AppConfig> {
                coEvery { this@declareMock.app.uniqueUsersAcrossOrganizations } returns false
            }

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/login"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        HttpHeaders.Authorization,
                        authHeader
                    )
                }
            ) {
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
            }
        }
    }

    @Nested
    @DisplayName("Login with Uniqueness flag as true")
    inner class LoginUnique {
        @BeforeEach
        fun setUniquenessFlag() {
            declareMock<AppConfig.App> {
                coEvery { this@declareMock.uniqueUsersAcrossOrganizations } returns true
            }
        }

        @Test
        fun `User not found`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                val email = "not-present-${createdUser.email}"
                val authString = "$email:${createdUser.passwordHash}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/login"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `Invalid Credentials`() {
            val invalidPassword = "some_invalid_pass"
            // Override the cognito mock to throw error for invalid username
            declareMock<CognitoIdentityProviderClient> {
                coEvery { this@declareMock.createUserPool(any<CreateUserPoolRequest>()) } coAnswers {
                    val result = CreateUserPoolResponse.builder()
                        .userPool(UserPoolType.builder().id("testUserPoolId").name("testUserPoolName").build()).build()
                    result
                }
                coEvery { this@declareMock.adminCreateUser(any<AdminCreateUserRequest>()) } coAnswers {
                    AdminCreateUserResponse.builder()
                        .user(
                            UserType.builder().attributes(listOf())
                                .username(firstArg<AdminCreateUserRequest>().username())
                                .userCreateDate(Instant.now())
                                .attributes(firstArg<AdminCreateUserRequest>().userAttributes())
                                .build()
                        )
                        .build()
                }
                coEvery { this@declareMock.adminInitiateAuth(match<AdminInitiateAuthRequest> {
                    it.authParameters()["PASSWORD"] == invalidPassword
                })
                } throws NotAuthorizedException.builder().message("Invalid username/password combo from user").build()

                coEvery { this@declareMock.adminInitiateAuth(match<AdminInitiateAuthRequest> {
                    it.authParameters()["PASSWORD"] != invalidPassword
                }) } coAnswers {
                    AdminInitiateAuthResponse.builder()
                        .session("").build()
                }

                coEvery { this@declareMock.createUserPoolClient(any<CreateUserPoolClientRequest>()) } coAnswers {
                    CreateUserPoolClientResponse.builder()
                        .userPoolClient(UserPoolClientType.builder().clientId("12345").build()).build()
                }
                coEvery {
                    this@declareMock.adminRespondToAuthChallenge(
                        any<AdminRespondToAuthChallengeRequest>()
                    )
                } returns mockk()

                coEvery { this@declareMock.adminGetUser(any<AdminGetUserRequest>()) } coAnswers {
                    AdminGetUserResponse.builder()
                        .enabled(true)
                        .userAttributes(listOf())
                        .username(firstArg<AdminGetUserRequest>().username())
                        .userCreateDate(Instant.now())
                        .build()
                }
                coEvery { this@declareMock.addCustomAttributes(any<AddCustomAttributesRequest>()) } returns mockk()
                coEvery { this@declareMock.deleteUserPool(any<DeleteUserPoolRequest>()) } returns DeleteUserPoolResponse
                    .builder().build()
            }

            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                val authString = "${createdUser.email}:$invalidPassword"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/login"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
            }
        }

        @Test
        fun `Valid Credentials`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                val authString = "${createdUser.email}:${createdUser.passwordHash}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/login"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    Assertions.assertEquals(
                        createdOrganization.organization?.id,
                        response.headers[Constants.X_ORGANIZATION_HEADER]
                    )
                    val responseBody = gson.fromJson(response.content, TokenResponse::class.java)
                    Assertions.assertNotNull(responseBody.token)
                }
            }
        }
    }
}

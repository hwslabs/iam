package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV2.createResourceActionHrn
import com.hypto.iam.server.idp.IdentityGroup
import com.hypto.iam.server.idp.IdentityProvider
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.GetDelegateTokenRequest
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.ResourceAction
import com.hypto.iam.server.models.ResourceActionEffect
import com.hypto.iam.server.models.RootUser
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.models.ValidationRequest
import com.hypto.iam.server.models.ValidationResponse
import com.hypto.iam.server.service.MasterKeyCache
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import io.jsonwebtoken.Claims
import io.jsonwebtoken.CompressionCodecs
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.ktor.client.request.get
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
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.util.Base64
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.test.inject
import org.koin.test.mock.declareMock
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AddCustomAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType

class TokenApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Nested
    @DisplayName("Generate JWT token test: /token")
    inner class GenerateJwtTokenWithoutOrgIdToken {
        @Test
        fun `generate token - Accept Json`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()
                val username = createdOrganization.organization.rootUser.username
                val tokenResponse = client.post(
                    "/token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = gson.fromJson(tokenResponse.bodyAsText(), TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                val validateResponse = client.post(
                    "/validate"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                    setBody(
                        gson.toJson(
                            ValidationRequest(
                                listOf(
                                    ResourceAction(
                                        ResourceHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            resourceInstance = username
                                        ).toString(),
                                        ActionHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            action = "createCredentials"
                                        ).toString()
                                    )
                                )
                            )
                        )
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, validateResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    validateResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    validateResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val validationResponseBody = gson.fromJson(
                    validateResponse.bodyAsText(),
                    ValidationResponse::class.java
                )
                validationResponseBody.results.forEach {
                    Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                }
            }
        }

        @Test
        fun `generate token - Accept Text_Plain`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()
                val username = createdOrganization.organization.rootUser.username
                val tokenResponse = client.post(
                    "/token"
                ) {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = tokenResponse.bodyAsText()
                Assertions.assertNotNull(responseBody)

                val validateResponse = client.post(
                    "/validate"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $responseBody")
                    setBody(
                        gson.toJson(
                            ValidationRequest(
                                listOf(
                                    ResourceAction(
                                        ResourceHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            resourceInstance = username
                                        ).toString(),
                                        ActionHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            action = "createCredentials"
                                        ).toString()
                                    )
                                )
                            )
                        )
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, validateResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    validateResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    validateResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val validationResponseBody = gson.fromJson(
                    validateResponse.bodyAsText(),
                    ValidationResponse::class.java
                )
                validationResponseBody.results.forEach {
                    Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                }
            }
        }

        @Test
        fun `Basic Auth with Uniqueness flag as false`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()

                val authString = "${createdUser.email}:${createdUser.password}"
                val authHeader = "Basic ${Base64.getEncoder().encode(authString.encodeToByteArray())}"

                declareMock<AppConfig> {
                    every { this@declareMock.app } answers {
                        AppConfig.App(
                            AppConfig.Environment.Development,
                            "IAM",
                            "local",
                            300,
                            600,
                            "iam-secret-key",
                            30,
                            30,
                            600,
                            5,
                            "https://localhost",
                            "mail@iam.com",
                            "signupTemplateId",
                            "inviteUserTemplateId",
                            "resetPasswordTemplateId",
                            false
                        )
                    }
                }

                val tokenResponse = client.post(
                    "/token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        authHeader
                    )
                }
                Assertions.assertEquals(HttpStatusCode.BadRequest, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
            }
        }

        @Nested
        @DisplayName("Basic Auth with Uniqueness flag as true")
        inner class LoginUnique {
            @BeforeEach
            fun setUniquenessFlag() {
                declareMock<AppConfig> {
                    every { this@declareMock.app } answers {
                        AppConfig.App(
                            AppConfig.Environment.Development,
                            "IAM",
                            "local",
                            300,
                            600,
                            "iam-secret-key",
                            30,
                            30,
                            600,
                            5,
                            "https://localhost",
                            "mail@iam.com",
                            "signupTemplateId",
                            "inviteUserTemplateId",
                            "resetPasswordTemplateId",
                            true
                        )
                    }
                    every { this@declareMock.cognito } answers {
                        IdentityGroup(
                            id = "us-east-1_id",
                            name = "user-pool-name",
                            identitySource = IdentityProvider.IdentitySource.AWS_COGNITO,
                            metadata = mapOf("iam-client-id" to "id")
                        )
                    }
                }
            }

            @Test
            fun `Valid Credentials`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val authString = "${createdUser.email}:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val tokenResponse = client.post(
                        "/token"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }

                    Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        tokenResponse.contentType()
                    )
                    Assertions.assertEquals(
                        createdOrganization.organization.id,
                        tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                    )
                    val responseBody = gson.fromJson(tokenResponse.bodyAsText(), TokenResponse::class.java)
                    Assertions.assertNotNull(responseBody.token)
                }
            }

            @Test
            fun `Invalid User`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val email = ""
                    val authString = "$email:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val tokenResponse = client.post(
                        "/token"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }

                    Assertions.assertEquals(HttpStatusCode.BadRequest, tokenResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        tokenResponse.contentType()
                    )
                }
            }

            @Test
            fun `Invalid Password`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val email = createdUser.email
                    val password = ""
                    val authString = "$email:$password"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val tokenResponse = client.post(
                        "/token"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }

                    Assertions.assertEquals(HttpStatusCode.BadRequest, tokenResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        tokenResponse.contentType()
                    )
                }
            }

            @Test
            fun `User not present`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val email = "not-present-${createdUser.email}"
                    val authString = "$email:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val tokenResponse = client.post(
                        "/token"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, tokenResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        tokenResponse.contentType()
                    )
                }
            }

            @Test
            fun `Incorrect Password`() {
                val invalidPassword = "some_invalid_pass"
                // Override the cognito mock to throw error for invalid username
                declareMock<CognitoIdentityProviderClient> {
                    coEvery { this@declareMock.createUserPool(any<CreateUserPoolRequest>()) } coAnswers {
                        val result = CreateUserPoolResponse.builder()
                            .userPool(
                                UserPoolType.builder().id("testUserPoolId").name("testUserPoolName").build()
                            ).build()
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
                    coEvery {
                        this@declareMock.adminInitiateAuth(
                            match<AdminInitiateAuthRequest> {
                                it.authParameters()["PASSWORD"] == invalidPassword
                            }
                        )
                    } throws NotAuthorizedException.builder()
                        .message("Invalid username and password combination").build()

                    coEvery {
                        this@declareMock.adminInitiateAuth(
                            match<AdminInitiateAuthRequest> {
                                it.authParameters()["PASSWORD"] != invalidPassword
                            }
                        )
                    } coAnswers {
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
                    coEvery { this@declareMock.deleteUserPool(any<DeleteUserPoolRequest>()) } returns
                        DeleteUserPoolResponse.builder().build()
                }

                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val authString = "${createdUser.email}:$invalidPassword"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val tokenResponse = client.post(
                        "/token"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, tokenResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        tokenResponse.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Generate JWT token test: /login")
    inner class GenerateJwtTokenWithoutOrgIdByLogin {
        @Test
        fun `generate token for case insensitive email credential`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val authString = "${createdUser.email.uppercase()}:${createdUser.password}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"
                val response = client.post(
                    "/organizations/${createdOrganization.id}/token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, authHeader)
                }
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER]
                )
            }
        }

        @Test
        fun `generate token - Accept Json`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()
                val username = createdOrganization.organization.rootUser.username
                val loginResponse = client.post(
                    "/login"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }

                Assertions.assertEquals(HttpStatusCode.OK, loginResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    loginResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    loginResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = gson.fromJson(loginResponse.bodyAsText(), TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                val validateResponse = client.post(
                    "/validate"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                    setBody(
                        gson.toJson(
                            ValidationRequest(
                                listOf(
                                    ResourceAction(
                                        ResourceHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            resourceInstance = username
                                        ).toString(),
                                        ActionHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            action = "createCredentials"
                                        ).toString()
                                    )
                                )
                            )
                        )
                    )
                }

                Assertions.assertEquals(HttpStatusCode.OK, validateResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    validateResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    validateResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val validationResponseBody = gson.fromJson(
                    validateResponse.bodyAsText(),
                    ValidationResponse::class.java
                )
                validationResponseBody.results.forEach {
                    Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                }
            }
        }

        @Test
        fun `generate token - Accept Text_Plain`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()
                val username = createdOrganization.organization.rootUser.username
                val loginResponse = client.post(
                    "/login"
                ) {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, loginResponse.status)
                Assertions.assertEquals(
                    ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                    loginResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    loginResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = loginResponse.bodyAsText()
                Assertions.assertNotNull(responseBody)

                val validateResponse = client.post(
                    "/validate"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $responseBody")
                    setBody(
                        gson.toJson(
                            ValidationRequest(
                                listOf(
                                    ResourceAction(
                                        ResourceHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            resourceInstance = username
                                        ).toString(),
                                        ActionHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            action = "createCredentials"
                                        ).toString()
                                    )
                                )
                            )
                        )
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, validateResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    validateResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    validateResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val validationResponseBody = gson.fromJson(
                    validateResponse.bodyAsText(),
                    ValidationResponse::class.java
                )
                validationResponseBody.results.forEach {
                    Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                }
            }
        }

        @Test
        fun `Basic Auth with Uniqueness flag as false`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()

                val authString = "${createdUser.email}:${createdUser.password}"
                val authHeader = "Basic ${Base64.getEncoder().encode(authString.encodeToByteArray())}"

                declareMock<AppConfig> {
                    every { this@declareMock.app } answers {
                        AppConfig.App(
                            AppConfig.Environment.Development,
                            "IAM",
                            "local",
                            300,
                            600,
                            "iam-secret-key",
                            30,
                            30,
                            600,
                            5,
                            "https://localhost",
                            "mail@iam.com",
                            "signupTemplateId",
                            "inviteUserTemplateId",
                            "resetPasswordTemplateId",
                            false
                        )
                    }
                }

                val loginResponse = client.post(
                    "/login"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        authHeader
                    )
                }
                Assertions.assertEquals(HttpStatusCode.BadRequest, loginResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    loginResponse.contentType()
                )
            }
        }

        @Nested
        @DisplayName("Basic Auth with Uniqueness flag as true")
        inner class LoginUnique {
            @BeforeEach
            fun setUniquenessFlag() {
                declareMock<AppConfig> {
                    every { this@declareMock.app } answers {
                        AppConfig.App(
                            AppConfig.Environment.Development,
                            "IAM",
                            "local",
                            300,
                            600,
                            "iam-secret-key",
                            30,
                            30,
                            600,
                            5,
                            "https://localhost",
                            "mail@iam.com",
                            "signupTemplateId",
                            "inviteUserTemplateId",
                            "resetPasswordTemplateId",
                            true
                        )
                    }
                    every { this@declareMock.cognito } answers {
                        IdentityGroup(
                            id = "us-east-1_id",
                            name = "user-pool-name",
                            identitySource = IdentityProvider.IdentitySource.AWS_COGNITO,
                            metadata = mapOf("iam-client-id" to "id")
                        )
                    }
                }
            }

            @Test
            fun `Valid Credentials`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val authString = "${createdUser.email}:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val loginResponse = client.post(
                        "/login"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }

                    Assertions.assertEquals(HttpStatusCode.OK, loginResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        loginResponse.contentType()
                    )
                    Assertions.assertEquals(
                        createdOrganization.organization.id,
                        loginResponse.headers[Constants.X_ORGANIZATION_HEADER]
                    )
                    val responseBody = gson.fromJson(loginResponse.bodyAsText(), TokenResponse::class.java)
                    Assertions.assertNotNull(responseBody.token)
                }
            }

            @Test
            fun `Invalid User`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val email = ""
                    val authString = "$email:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val loginResponse = client.post(
                        "/login"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                    Assertions.assertEquals(HttpStatusCode.BadRequest, loginResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        loginResponse.contentType()
                    )
                }
            }

            @Test
            fun `Invalid Password`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val email = createdUser.email
                    val password = ""
                    val authString = "$email:$password"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val loginResponse = client.post(
                        "/login"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                    Assertions.assertEquals(HttpStatusCode.BadRequest, loginResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        loginResponse.contentType()
                    )
                }
            }

            @Test
            fun `User not present`() {
                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val email = "not-present-${createdUser.email}"
                    val authString = "$email:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val loginResponse = client.post(
                        "/login"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, loginResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        loginResponse.contentType()
                    )
                }
            }

            @Test
            fun `Incorrect Password`() {
                val invalidPassword = "some_invalid_pass"
                // Override the cognito mock to throw error for invalid username
                declareMock<CognitoIdentityProviderClient> {
                    coEvery { this@declareMock.createUserPool(any<CreateUserPoolRequest>()) } coAnswers {
                        val result = CreateUserPoolResponse.builder()
                            .userPool(
                                UserPoolType.builder().id("testUserPoolId").name("testUserPoolName").build()
                            ).build()
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
                    coEvery {
                        this@declareMock.adminInitiateAuth(
                            match<AdminInitiateAuthRequest> {
                                it.authParameters()["PASSWORD"] == invalidPassword
                            }
                        )
                    } throws NotAuthorizedException.builder()
                        .message("Invalid username and password combination").build()

                    coEvery {
                        this@declareMock.adminInitiateAuth(
                            match<AdminInitiateAuthRequest> {
                                it.authParameters()["PASSWORD"] != invalidPassword
                            }
                        )
                    } coAnswers {
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
                    coEvery { this@declareMock.deleteUserPool(any<DeleteUserPoolRequest>()) } returns
                        DeleteUserPoolResponse.builder().build()
                }

                testApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                    val (createdOrganization, createdUser) = createOrganization()

                    val authString = "${createdUser.email}:$invalidPassword"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    val loginResponse = client.post(
                        "/login"
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader
                        )
                    }
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, loginResponse.status)
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        loginResponse.contentType()
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Generate JWT token test: /organization/:id/token")
    inner class GenerateJwtTokenWithOrgId {
        @Test
        fun `generate and validate action with token - without key rotation`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()
                val username = createdOrganization.organization.rootUser.username
                val tokenResponse = client.post(
                    "/organizations/${createdOrganization.organization.id}/token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = gson.fromJson(tokenResponse.bodyAsText(), TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                val validateResponse = client.post(
                    "/validate"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                    setBody(
                        gson.toJson(
                            ValidationRequest(
                                listOf(
                                    ResourceAction(
                                        ResourceHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            resourceInstance = username
                                        ).toString(),
                                        ActionHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            action = "createCredentials"
                                        ).toString()
                                    )
                                )
                            )
                        )
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, validateResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    validateResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    validateResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val validationResponseBody = gson.fromJson(
                    validateResponse.bodyAsText(),
                    ValidationResponse::class.java
                )
                validationResponseBody.results.forEach {
                    Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                }
            }
        }

        @Test
        fun `generate token with basic credentials`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, rootUser) = createOrganization()
                val authString = "${rootUser.email}:${rootUser.password}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                val tokenResponse = client.post(
                    "/organizations/${createdOrganization.organization.id}/token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, authHeader)
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
            }
        }

        @Test
        fun `generate token and validate action after key rotation`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()
                val username = createdOrganization.organization.rootUser.username

                val tokenResponse = client.post(
                    "/organizations/${createdOrganization.organization.id}/token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = gson.fromJson(tokenResponse.bodyAsText(), TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                // TODO: Expose key rotation as an API and invoke it
                val masterKeysRepo by inject<MasterKeysRepo>()
                runBlocking {
                    masterKeysRepo.rotateKey()
                }

                val validateResponse = client.post(
                    "/validate"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                    setBody(
                        gson.toJson(
                            ValidationRequest(
                                listOf(
                                    ResourceAction(
                                        ResourceHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            resourceInstance = username
                                        ).toString(),
                                        ActionHrn(
                                            organization = createdOrganization.organization.id,
                                            resource = IamResources.USER,
                                            action = "createCredentials"
                                        ).toString()
                                    )
                                )
                            )
                        )
                    )
                }
                Assertions.assertEquals(HttpStatusCode.OK, validateResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    validateResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    validateResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val validationResponseBody = gson.fromJson(
                    validateResponse.bodyAsText(),
                    ValidationResponse::class.java
                )
                validationResponseBody.results.forEach {
                    Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                }
            }
        }
    }

    @Suppress("LongParameterList")
    @Nested
    @DisplayName("Validate JWT token test")
    inner class ValidateJwtToken {
        private val masterKeyCache: MasterKeyCache by inject()

        private fun generateToken(
            createdOrganizationResponse: CreateOrganizationResponse,
            createdUser: RootUser,
            issuedAt: Date = Date(),
            issuer: String = TokenServiceImpl.ISSUER,
            userHrn: String = ResourceHrn(
                organization = createdOrganizationResponse.organization.id,
                resource = IamResources.USER,
                resourceInstance = createdOrganizationResponse.organization.rootUser.username
            ).toString(),
            organization: String? = createdOrganizationResponse.organization.name,
            expiration: Date = Date.from(Instant.now().plusSeconds(100)),
            version: String? = TokenServiceImpl.VERSION_NUM
        ): String {
            val signingKey = runBlocking {
                masterKeyCache.forSigning()
            }
            return Jwts.builder()
                .setHeaderParam(TokenServiceImpl.KEY_ID, signingKey.id)
                .setIssuedAt(issuedAt)
                .setIssuer(issuer)
                .claim(TokenServiceImpl.USER_CLAIM, userHrn)
                .setExpiration(expiration)
                .claim(TokenServiceImpl.ORGANIZATION_CLAIM, organization)
                .claim(TokenServiceImpl.VERSION_CLAIM, version)
                .claim(TokenServiceImpl.ENTITLEMENTS_CLAIM, "TEST_CLAIM")
                .signWith(signingKey.privateKey, SignatureAlgorithm.ES256)
                .compressWith(CompressionCodecs.GZIP)
                .compact()
        }

        @Test
        fun `Token Expired`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createOrganizationResponse, createdUser) = createOrganization()
                val expiry = Date.from(Instant.now().minusSeconds(100))
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    expiration = expiry
                )

                // Act
                val response = client.get(
                    "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $jwt"
                    )
                }
                // Assert
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Invalid issuer`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createOrganizationResponse, createdUser) = createOrganization()
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    issuer = "Invalid_Issuer"
                )

                // Act
                val response = client.get(
                    "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $jwt"
                    )
                }
                // Assert
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Invalid userHrn`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createOrganizationResponse, createdUser) = createOrganization()
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    userHrn = "InvalidHrnFormat"
                )

                // Act
                val response = client.get(
                    "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $jwt"
                    )
                }
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Invalid Organization`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createOrganizationResponse, createdUser) = createOrganization()
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    organization = null
                )

                // Act
                val response = client.get(
                    "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $jwt"
                    )
                }
                // Assert
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Invalid version`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createOrganizationResponse, createdUser) = createOrganization()
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    version = null
                )

                // Act
                val response = client.get(
                    "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $jwt"
                    )
                }
                // Assert
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `Invalid issuedAt`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createOrganizationResponse, createdUser) = createOrganization()
                val issuedAt = Date.from(Instant.now().plusSeconds(1000))
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    issuedAt = issuedAt
                )

                // Act
                val response = client.get(
                    "/organizations/${createOrganizationResponse.organization.id}/policies/non_existing_policy"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $jwt"
                    )
                }
                // Assert
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    @Nested
    @DisplayName("Generate Delegate JWT token test: /delegate_token")
    inner class GenerateDelegateToken {
        @Test
        fun `generate delegate token - Accept Json`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()

                // Create policy to delegate
                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = createResourceActionHrn(
                    createdOrganization.organization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val createPolictRequest = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = client.post(
                    "/organizations/${createdOrganization.organization.id}/policies"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                    setBody(gson.toJson(createPolictRequest))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.bodyAsText(), Policy::class.java)

                // Test DelegateToken call
                val requestBody = GetDelegateTokenRequest(
                    policy = createdPolicy.hrn,
                    principal = "dummyDelegate",
                    expiry = 100L
                )
                val tokenResponse = client.post(
                    "/delegate_token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                    setBody(gson.toJson(requestBody))
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
                val responseBody = gson.fromJson(tokenResponse.bodyAsText(), TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                val tokenWithoutSignaturePart =
                    responseBody.token.substring(0, responseBody.token.lastIndexOf(".") + 1)

                // Assertion permissions to principal by validating
                val claims = Jwts.parserBuilder().build().parseClaimsJwt(tokenWithoutSignaturePart).body as Claims
                Assertions.assertEquals(claims.get("usr", String::class.java) as String, "dummyDelegate")
                Assertions.assertEquals(
                    claims.get("org", String::class.java) as String,
                    createdOrganization.organization.id
                )
                Assertions.assertEquals(
                    claims.get("obof", String::class.java) as String,
                    createdOrganization.organization.rootUser.hrn
                )
                Assertions.assertEquals(
                    claims.expiration.toInstant().epochSecond - claims.issuedAt.toInstant().epochSecond,
                    100L
                )

                Assertions.assertTrue(
                    (claims.get("entitlements", String::class.java) as String)
                        .contains("g, dummyDelegate, ${createdPolicy.hrn}")
                )
            }
        }

        @Test
        fun `generate delegate token - Accept Text_Plain`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()

                // Create policy to delegate
                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = createResourceActionHrn(
                    createdOrganization.organization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val createPolictRequest = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = client.post(
                    "/organizations/${createdOrganization.organization.id}/policies"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                    setBody(gson.toJson(createPolictRequest))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.bodyAsText(), Policy::class.java)

                // Test DelegateToken call
                val requestBody = GetDelegateTokenRequest(
                    policy = createdPolicy.hrn,
                    principal = "dummyDelegate",
                    expiry = 100L
                )
                val tokenResponse = client.post(
                    "/delegate_token"
                ) {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.toString())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                    setBody(gson.toJson(requestBody))
                }
                Assertions.assertEquals(HttpStatusCode.OK, tokenResponse.status)
                Assertions.assertEquals(
                    ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                    tokenResponse.contentType()
                )
                Assertions.assertEquals(
                    createdOrganization.organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER]
                )
            }
        }

        @Test
        fun `generate delegate token - JWT requester, without expiry`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganization, createdUser) = createOrganization()

                // Create policy to delegate
                val policyName = "samplePolicy"
                val resourceName = "resource"
                val (resourceHrn, actionHrn) = createResourceActionHrn(
                    createdOrganization.organization.id,
                    null,
                    resourceName,
                    "action"
                )
                val policyStatements = listOf(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
                val createPolicyRequest = CreatePolicyRequest(policyName, policyStatements)

                val createPolicyCall = client.post(
                    "/organizations/${createdOrganization.organization.id}/policies"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                    setBody(gson.toJson(createPolicyRequest))
                }

                val createdPolicy = gson.fromJson(createPolicyCall.bodyAsText(), Policy::class.java)

                // Test DelegateToken call
                val requestBody = GetDelegateTokenRequest(
                    policy = createdPolicy.hrn,
                    principal = "dummyDelegate"
                )
                val tokenResponse = client.post(
                    "/delegate_token"
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                    setBody(gson.toJson(requestBody))
                }
                val rootUserClaims = with(createdOrganization.rootUserToken) {
                    Jwts.parserBuilder().build().parseClaimsJwt(substring(0, lastIndexOf(".") + 1)).body as Claims
                }

                val responseBody = gson.fromJson(tokenResponse.bodyAsText(), TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                val tokenWithoutSignaturePart =
                    responseBody.token.substring(0, responseBody.token.lastIndexOf(".") + 1)

                // Assertion permissions to principal by validating
                val claims = Jwts.parserBuilder().build().parseClaimsJwt(tokenWithoutSignaturePart).body as Claims
                Assertions.assertEquals(claims.get("usr", String::class.java) as String, "dummyDelegate")
                Assertions.assertEquals(
                    claims.get("org", String::class.java) as String,
                    createdOrganization.organization.id
                )
                Assertions.assertEquals(
                    claims.get("obof", String::class.java) as String,
                    createdOrganization.organization.rootUser.hrn
                )
                Assertions.assertEquals(claims.expiration, rootUserClaims.expiration)

                Assertions.assertTrue(
                    (claims.get("entitlements", String::class.java) as String)
                        .contains("g, dummyDelegate, ${createdPolicy.hrn}")
                )
            }
        }

        /**
         * TODO: Cases to add
         * - generate delegate token - Credential requester, with expiry
         * - generate delegate token - Credential requester, without expiry
         * - generate delegate token - policy does not exist
         * - generate delegate token - requestor does not have delegate_policy permission
         */
    }
}

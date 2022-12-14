package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.ErrorMessageStrings
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.ErrorResponse
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
import io.jsonwebtoken.CompressionCodecs
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
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
    inner class GenerateJwtTokenWithoutOrgId {
        @Test
        fun `generate token - Accept Json`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)
                val username = createdOrganization.organization.rootUser.username
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/token"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganization.rootUserToken}"
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

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/validate"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                            setBody(
                                gson.toJson(
                                    ValidationRequest(
                                        listOf(
                                            ResourceAction(
                                                ResourceHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    resourceInstance = username
                                                ).toString(),
                                                ActionHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    action = "createCredentials"
                                                ).toString()
                                            )
                                        )
                                    )
                                )
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
                        val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                        validationResponseBody.results.forEach {
                            Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                        }
                    }
                }
            }
        }

        @Test
        fun `generate token - Accept Text_Plain`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)
                val username = createdOrganization.organization.rootUser.username
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/token"
                    ) {
                        addHeader(HttpHeaders.Accept, ContentType.Text.Plain.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganization.rootUserToken}"
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    Assertions.assertEquals(
                        createdOrganization.organization!!.id,
                        response.headers[Constants.X_ORGANIZATION_HEADER]
                    )
                    val responseBody = response.content
                    Assertions.assertNotNull(responseBody)

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/validate"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, "Bearer $responseBody")
                            setBody(
                                gson.toJson(
                                    ValidationRequest(
                                        listOf(
                                            ResourceAction(
                                                ResourceHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    resourceInstance = username
                                                ).toString(),
                                                ActionHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    action = "createCredentials"
                                                ).toString()
                                            )
                                        )
                                    )
                                )
                            )
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.OK, response.status())
                        Assertions.assertEquals(
                            ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            response.contentType()
                        )
                        Assertions.assertEquals(
                            createdOrganization.organization!!.id,
                            response.headers[Constants.X_ORGANIZATION_HEADER]
                        )
                        val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                        validationResponseBody.results.forEach {
                            Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                        }
                    }
                }
            }
        }

        @Test
        fun `Basic Auth with Uniqueness flag as false`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

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
                            "resetPasswordTemplateId",
                            false
                        )
                    }
                }

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/token"
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
                            "resetPasswordTemplateId",
                            true
                        )
                    }
                }
            }

            @Test
            fun `Valid Credentials`() {
                withTestApplication(Application::handleRequest) {
                    val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                    val authString = "${createdUser.email}:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/token"
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

            @Test
            fun `Invalid User`() {
                withTestApplication(Application::handleRequest) {
                    val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                    val email = ""
                    val authString = "$email:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/token"
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
            fun `Invalid Password`() {
                withTestApplication(Application::handleRequest) {
                    val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                    val email = createdUser.email
                    val password = ""
                    val authString = "$email:$password"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/token"
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
            fun `User not present`() {
                withTestApplication(Application::handleRequest) {
                    val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                    val email = "not-present-${createdUser.email}"
                    val authString = "$email:${createdUser.password}"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/token"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(
                                HttpHeaders.Authorization,
                                authHeader
                            )
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status())
                        Assertions.assertEquals(
                            ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            response.contentType()
                        )
                    }
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

                withTestApplication(Application::handleRequest) {
                    val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

                    val authString = "${createdUser.email}:$invalidPassword"
                    val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/token"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(
                                HttpHeaders.Authorization,
                                authHeader
                            )
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status())
                        Assertions.assertEquals(
                            ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            response.contentType()
                        )
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Generate JWT token test: /organization/:id/token")
    inner class GenerateJwtTokenWithOrgId {
        @Test
        fun `generate and validate action with token - without key rotation`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)
                val username = createdOrganization.organization.rootUser.username
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.organization?.id}/token"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganization.rootUserToken}"
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

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/validate"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                            setBody(
                                gson.toJson(
                                    ValidationRequest(
                                        listOf(
                                            ResourceAction(
                                                ResourceHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    resourceInstance = username
                                                ).toString(),
                                                ActionHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    action = "createCredentials"
                                                ).toString()
                                            )
                                        )
                                    )
                                )
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
                        val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                        validationResponseBody.results.forEach {
                            Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                        }
                    }
                }
            }
        }

        @Test
        fun `generate token with basic credentials`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, rootUser) = DataSetupHelper.createOrganization(this)
                val authString = "${rootUser.email}:${rootUser.password}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.organization.id}/token"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, authHeader)
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    Assertions.assertEquals(
                        createdOrganization.organization.id,
                        response.headers[Constants.X_ORGANIZATION_HEADER]
                    )
                }
            }
        }

        @Test
        fun `generate token and validate action after key rotation`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)
                val username = createdOrganization.organization.rootUser.username

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.organization?.id}/token"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer ${createdOrganization.rootUserToken}"
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

                    // TODO: Expose key rotation as an API and invoke it
                    val masterKeysRepo by inject<MasterKeysRepo>()
                    runBlocking {
                        masterKeysRepo.rotateKey()
                    }

                    with(
                        handleRequest(
                            HttpMethod.Post,
                            "/validate"
                        ) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, "Bearer ${responseBody.token}")
                            setBody(
                                gson.toJson(
                                    ValidationRequest(
                                        listOf(
                                            ResourceAction(
                                                ResourceHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    resourceInstance = username
                                                ).toString(),
                                                ActionHrn(
                                                    organization = createdOrganization.organization!!.id,
                                                    resource = IamResources.USER,
                                                    action = "createCredentials"
                                                ).toString()
                                            )
                                        )
                                    )
                                )
                            )
                        }
                    ) {
                        Assertions.assertEquals(HttpStatusCode.OK, response.status())
                        Assertions.assertEquals(
                            ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            response.contentType()
                        )
                        Assertions.assertEquals(
                            createdOrganization.organization!!.id,
                            response.headers[Constants.X_ORGANIZATION_HEADER]
                        )
                        val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                        validationResponseBody.results.forEach {
                            Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                        }
                    }
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
                organization = createdOrganizationResponse.organization!!.id,
                resource = IamResources.USER,
                resourceInstance = createdOrganizationResponse.organization.rootUser.username
            ).toString(),
            organization: String? = createdOrganizationResponse.organization!!.name,
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
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val expiry = Date.from(Instant.now().minusSeconds(100))
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    expiration = expiry
                )

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer $jwt"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val responseBody = gson.fromJson(response.content, ErrorResponse::class.java)
                    Assertions.assertTrue(responseBody.message.contains("JWT expired"))
                }
            }
        }

        @Test
        fun `Invalid issuer`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    issuer = "Invalid_Issuer"
                )

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer $jwt"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val responseBody = gson.fromJson(response.content, ErrorResponse::class.java)
                    Assertions.assertEquals(ErrorMessageStrings.JWT_INVALID_ISSUER, responseBody.message)
                }
            }
        }

        @Test
        fun `Invalid userHrn`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    userHrn = "InvalidHrnFormat"
                )

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer $jwt"
                        )
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val responseBody = gson.fromJson(response.content, ErrorResponse::class.java)
                    Assertions.assertEquals(ErrorMessageStrings.JWT_INVALID_USER_HRN, responseBody.message)
                }
            }
        }

        @Test
        fun `Invalid Organization`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    organization = null
                )

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer $jwt"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val responseBody = gson.fromJson(response.content, ErrorResponse::class.java)
                    Assertions.assertEquals(ErrorMessageStrings.JWT_INVALID_ORGANIZATION, responseBody.message)
                }
            }
        }

        @Test
        fun `Invalid version`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    version = null
                )

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer $jwt"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val responseBody = gson.fromJson(response.content, ErrorResponse::class.java)
                    Assertions.assertEquals(ErrorMessageStrings.JWT_INVALID_VERSION_NUMBER, responseBody.message)
                }
            }
        }

        @Test
        fun `Invalid issuedAt`() {
            withTestApplication(Application::handleRequest) {
                val (createOrganizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val issuedAt = Date.from(Instant.now().plusSeconds(1000))
                val jwt = generateToken(
                    createdOrganizationResponse = createOrganizationResponse,
                    createdUser = createdUser,
                    issuedAt = issuedAt
                )

                // Act
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createOrganizationResponse.organization?.id}/policies/non_existing_policy"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            "Bearer $jwt"
                        )
                    }
                ) {
                    // Assert
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val responseBody = gson.fromJson(response.content, ErrorResponse::class.java)
                    Assertions.assertEquals(
                        String.format(ErrorMessageStrings.JWT_INVALID_ISSUED_AT, issuedAt),
                        responseBody.message
                    )
                }
            }
        }
    }
}

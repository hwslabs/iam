package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.ErrorMessageStrings
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.*
import com.hypto.iam.server.service.MasterKeyCache
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import io.jsonwebtoken.CompressionCodecs
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.Instant
import java.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.test.inject

class TokenApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `generate and validate action with token - without key rotation`() {
        withTestApplication(Application::handleRequest) {
            val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/token"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdOrganization.adminUserCredential?.secret}")
                }
            ) {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
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
                                                resourceInstance = createdUser.username
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
                    val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                    validationResponseBody.results.forEach {
                        Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
                    }
                }
            }
        }
    }

    @Test
    fun `generate token and validate action after key rotation`() {
        withTestApplication(Application::handleRequest) {
            val (createdOrganization, createdUser) = DataSetupHelper.createOrganization(this)

            with(
                handleRequest(
                    HttpMethod.Post,
                    "/token"
                ) {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdOrganization.adminUserCredential?.secret}")
                }
            ) {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                Assertions.assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                val responseBody = gson.fromJson(response.content, TokenResponse::class.java)
                Assertions.assertNotNull(responseBody.token)

                // TODO: Expose key rotation as an API and invoke it
                val masterKeysRepo by inject<MasterKeysRepo>()
                masterKeysRepo.rotateKey()

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
                                                resourceInstance = createdUser.username
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
                    val validationResponseBody = gson.fromJson(response.content, ValidationResponse::class.java)
                    validationResponseBody.results.forEach {
                        Assertions.assertEquals(ResourceActionEffect.Effect.allow, it.effect)
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
            createdUser: AdminUser,
            issuedAt: Date = Date(),
            issuer: String = TokenServiceImpl.ISSUER,
            userHrn: String = ResourceHrn(
                organization = createdOrganizationResponse.organization!!.id,
                resource = IamResources.USER,
                resourceInstance = createdUser.username
            ).toString(),
            organization: String? = createdOrganizationResponse.organization!!.name,
            expiration: Date = Date.from(Instant.now().plusSeconds(100)),
            version: String? = TokenServiceImpl.VERSION_NUM
        ): String {
            val signingKey = masterKeyCache.forSigning()
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
                    expiration = expiry)

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
                    issuer = "Invalid_Issuer")

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
                    userHrn = "InvalidHrnFormat")

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
                    organization = null)

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
                    version = null)

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
                    issuedAt = issuedAt)

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

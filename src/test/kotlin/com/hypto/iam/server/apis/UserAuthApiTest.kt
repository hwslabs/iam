package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.ROOT_ORG
import com.hypto.iam.server.authProviders.MicrosoftAuthProvider
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.models.AddUserAuthMethodRequest
import com.hypto.iam.server.models.UserAuthMethodsResponse
import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.service.TokenServiceImpl
import io.ktor.client.request.get
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
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.core.component.inject
import java.util.UUID

class UserAuthApiTest : AbstractContainerBaseTest() {
    val gson: Gson by inject()

    @Test
    fun `List auth methods`() {
        testApplication {
            // Arrange
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (createdOrganization, createdUser) = createOrganization()

            // Act
            val orgId = createdOrganization.organization.id
            val userId =
                createdOrganization.organization.rootUser.hrn.substring(
                    createdOrganization.organization.rootUser.hrn.lastIndexOf("/") + 1,
                )
            val response =
                client.get("/organizations/$orgId/users/$userId/auth_methods") {
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                }
            val userAuthMethodsResponse = gson.fromJson(response.bodyAsText(), UserAuthMethodsResponse::class.java)

            // Assert
            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            userAuthMethodsResponse.data?.let { Assertions.assertEquals(1, it.size) }
            userAuthMethodsResponse.data?.let { Assertions.assertEquals(TokenServiceImpl.ISSUER, it[0].providerName) }
        }
    }

    @Test
    fun `Add auth methods microsoft`() {
        testApplication {
            // Arrange
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (createdOrganization, createdUser) = createOrganization()
            val orgId = createdOrganization.organization.id
            val userId =
                createdOrganization.organization.rootUser.hrn.substring(
                    createdOrganization.organization.rootUser.hrn.lastIndexOf("/") + 1,
                )
            val microsoftToken = "test-microsoft-token"
            val id = UUID.randomUUID().toString()

            mockkObject(MicrosoftAuthProvider)
            coEvery {
                MicrosoftAuthProvider.getProfileDetails(any())
            } coAnswers {
                OAuthUserPrincipal(
                    tokenCredential = TokenCredential(microsoftToken, TokenType.OAUTH),
                    companyName = createdOrganization.organization.name,
                    name = createdUser.name ?: "",
                    email = createdUser.email,
                    organization = ROOT_ORG,
                    issuer = "microsoft",
                    metadata = AuthMetadata(id = id),
                )
            }

            // Act
            val addAuthMethodsResponse =
                client.post("/organizations/$orgId/users/$userId/auth_methods") {
                    header(HttpHeaders.ContentType, "application/json")
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                    setBody(
                        gson.toJson(
                            AddUserAuthMethodRequest(
                                issuer = "microsoft",
                                token = microsoftToken,
                            ),
                        ),
                    )
                }
            val listAuthMethodsResponse =
                client.get("/organizations/$orgId/users/$userId/auth_methods") {
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                }
            val userAuthMethodsResponse = gson.fromJson(listAuthMethodsResponse.bodyAsText(), UserAuthMethodsResponse::class.java)

            // Assert
            Assertions.assertEquals(HttpStatusCode.Created, addAuthMethodsResponse.status)
            Assertions.assertEquals(HttpStatusCode.OK, listAuthMethodsResponse.status)
            userAuthMethodsResponse.data?.let { Assertions.assertEquals(2, it.size) }
            userAuthMethodsResponse.data?.let { Assertions.assertEquals(TokenServiceImpl.ISSUER, it[0].providerName) }
            userAuthMethodsResponse.data?.let { Assertions.assertEquals("microsoft", it[1].providerName) }

            val loginResponse =
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("x-issuer", "microsoft")
                    header(HttpHeaders.Authorization, "Bearer $microsoftToken")
                }
            Assertions.assertEquals(HttpStatusCode.OK, loginResponse.status)
        }
    }

    @Test
    fun `Add auth methods - failure`() {
        testApplication {
            // Arrange
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (createdOrganization, createdUser) = createOrganization()
            val orgId = createdOrganization.organization.id
            val userId =
                createdOrganization.organization.rootUser.hrn.substring(
                    createdOrganization.organization.rootUser.hrn.lastIndexOf("/") + 1,
                )
            val microsoftToken = "test-microsoft-token"

            mockkObject(MicrosoftAuthProvider)
            coEvery {
                MicrosoftAuthProvider.getProfileDetails(any())
            } coAnswers {
                OAuthUserPrincipal(
                    tokenCredential = TokenCredential(microsoftToken, TokenType.OAUTH),
                    companyName = createdOrganization.organization.name,
                    name = createdUser.name ?: "",
                    email = createdUser.email,
                    organization = ROOT_ORG,
                    issuer = "microsoft",
                    metadata = AuthMetadata(id = UUID.randomUUID().toString()),
                )
            }

            // Act
            val addAuthMethodsResponse1 =
                client.post("/organizations/$orgId/users/$userId/auth_methods") {
                    header(HttpHeaders.ContentType, "application/json")
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                    setBody(
                        gson.toJson(
                            AddUserAuthMethodRequest(
                                issuer = "microsoft",
                            ),
                        ),
                    )
                }
            val addAuthMethodsResponse2 =
                client.post("/organizations/$orgId/users/$userId/auth_methods") {
                    header(HttpHeaders.ContentType, "application/json")
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                    setBody(
                        gson.toJson(
                            AddUserAuthMethodRequest(
                                token = microsoftToken,
                            ),
                        ),
                    )
                }
            val addAuthMethodsResponse3 =
                client.post("/organizations/$orgId/users/$userId/auth_methods") {
                    header(HttpHeaders.ContentType, "application/json")
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                    setBody(
                        gson.toJson(
                            AddUserAuthMethodRequest(
                                issuer = "msft",
                                token = microsoftToken,
                            ),
                        ),
                    )
                }

            // Assert
            Assertions.assertEquals(HttpStatusCode.BadRequest, addAuthMethodsResponse1.status)
            Assertions.assertEquals(HttpStatusCode.BadRequest, addAuthMethodsResponse2.status)
            Assertions.assertEquals(HttpStatusCode.BadRequest, addAuthMethodsResponse3.status)
        }
    }
}

package com.hypto.iam.server.apis

import com.hypto.iam.server.Constants
import com.hypto.iam.server.ROOT_ORG
import com.hypto.iam.server.authProviders.GoogleAuthProvider
import com.hypto.iam.server.authProviders.MicrosoftAuthProvider
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.helpers.BaseSingleAppTest
import com.hypto.iam.server.helpers.DataSetupHelper.deleteOrganization
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.models.PolicyPaginatedResponse
import com.hypto.iam.server.models.RootUser
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.models.UserPaginatedResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import io.ktor.test.dispatcher.testSuspend
import io.mockk.coEvery
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.inject
import org.koin.test.mock.declareMock
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@Testcontainers
internal class OrganizationApiKtTest : BaseSingleAppTest() {
    private val passcodeService: PasscodeService by inject()

    @Test
    fun `create organization with valid root credentials`() {
        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPassword = "testPassword@Hash1"

            lateinit var orgId: String
            val requestBody =
                CreateOrganizationRequest(
                    orgName,
                    RootUser(
                        preferredUsername = preferredUsername,
                        name = name,
                        password = testPassword,
                        email = testEmail,
                    ),
                )
            val response =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateOrganizationResponse::class.java)

            // Assert API response
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            orgId = responseBody.organization.id
            assertEquals(requestBody.name, responseBody.organization.name)
            assertEquals(10, responseBody.organization.id.length)

            // Assert root user creation
            val listUsersResponse =
                testApp.client.get("/organizations/$orgId/users") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${responseBody.rootUserToken}")
                }
            Assertions.assertEquals(HttpStatusCode.OK, listUsersResponse.status)
            Assertions.assertEquals(
                ContentType.Application.Json,
                listUsersResponse.contentType(),
            )

            val listUserResponse = gson.fromJson(listUsersResponse.bodyAsText(), UserPaginatedResponse::class.java)
            Assertions.assertEquals(listUserResponse.data!!.size, 1)

            // Assert policy creation
            val actResponse =
                testApp.client.get(
                    "/organizations/$orgId/policies?pageSize=50",
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${responseBody.rootUserToken}",
                    )
                }
            Assertions.assertEquals(HttpStatusCode.OK, actResponse.status)
            Assertions.assertEquals(
                ContentType.Application.Json,
                actResponse.contentType(),
            )

            val listPoliciesResponse = gson.fromJson(actResponse.bodyAsText(), PolicyPaginatedResponse::class.java)
            Assertions.assertNotNull(listPoliciesResponse.nextToken)
            Assertions.assertEquals(1, listPoliciesResponse.data?.size)
            Assertions.assertEquals("admin", listPoliciesResponse.data!![0].name)

            // Assert policy association
            val userPoliciesResponse =
                testApp.client.get(
                    "/organizations/$orgId/users/${responseBody.organization.rootUser.username}/policies",
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${responseBody.rootUserToken}")
                }
            Assertions.assertEquals(HttpStatusCode.OK, userPoliciesResponse.status)
            Assertions.assertEquals(
                ContentType.Application.Json,
                userPoliciesResponse.contentType(),
            )

            val policies = gson.fromJson(userPoliciesResponse.bodyAsText(), PolicyPaginatedResponse::class.java)
            Assertions.assertEquals(1, policies.data?.size)
            Assertions.assertEquals("admin", policies.data!![0].name)

            deleteOrganization(orgId)
        }
    }

    @Test
    fun `create organization with invalid root credentials`() {
        declareMock<PasscodeRepo> {
            coEvery {
                getValidPasscodeById(
                    any<String>(),
                    any<VerifyEmailRequest.Purpose>(),
                    any<String>(),
                )
            } returns null
        }
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test-name" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val response =
                client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", "bad creds")
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                RootUser(
                                    preferredUsername = preferredUsername,
                                    name = name,
                                    password = testPassword,
                                    email = testEmail,
                                    phone = testPhone,
                                ),
                            ),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertFalse(response.headers.contains(HttpHeaders.ContentType))
        }
    }

    @Test
    fun `create organization with verify email method`() {
        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"
            val testPasscode = "testPasscode"

            lateinit var orgId: String
            val verifyRequestBody =
                VerifyEmailRequest(
                    email = testEmail,
                    purpose = VerifyEmailRequest.Purpose.signup,
                )
            testApp.client.post("/verifyEmail") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(verifyRequestBody))
            }
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
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", testPasscode)
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateOrganizationResponse::class.java)
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(
                ContentType.Application.Json,
                response.contentType(),
            )

            orgId = responseBody.organization.id
            assertEquals(requestBody.name, responseBody.organization.name)
            assertEquals(10, responseBody.organization.id.length)

            deleteOrganization(orgId)
        }
    }

    @Test
    fun `create organization by providing org details during verify email`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919999999999"
            val testPassword = "testPassword@Hash1"
            val testPasscode = "testPasscode"
            val metadata =
                mapOf<String, Any>(
                    "name" to orgName,
                    "rootUserName" to name,
                    "rootUserPassword" to testPassword,
                    "rootUserVerified" to true,
                    "rootUserPreferredUsername" to preferredUsername,
                    "rootUserPhone" to testPhone,
                )

            coEvery {
                passcodeRepo.getValidPasscodeById(any(), VerifyEmailRequest.Purpose.signup)
            } coAnswers {
                PasscodesRecord().apply {
                    this.id = testPasscode
                    this.email = testEmail
                    this.purpose = VerifyEmailRequest.Purpose.signup.value
                    this.validUntil = LocalDateTime.MAX
                    this.metadata = passcodeService.encryptMetadata(metadata)
                }
            }

            lateinit var orgId: String
            val verifyRequestBody =
                VerifyEmailRequest(
                    email = testEmail,
                    purpose = VerifyEmailRequest.Purpose.signup,
                    metadata = metadata,
                )
            client.post("/verifyEmail") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(verifyRequestBody))
            }
            val response =
                client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", testPasscode)
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateOrganizationResponse::class.java)
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(
                ContentType.Application.Json,
                response.contentType(),
            )

            orgId = responseBody.organization.id
            assertEquals(orgName, responseBody.organization.name)
            assertEquals(10, responseBody.organization.id.length)

            deleteOrganization(orgId)
        }
    }

    @Test
    fun `create organization using google authorization - success`() {
        testSuspend {
            // Arrange
            val googleToken = "test-google-token"
            val name = "test-name"
            val email = "test-email"
            val companyName = "test-company"

            mockkObject(GoogleAuthProvider)
            coEvery {
                GoogleAuthProvider.getProfileDetails(any())
            } coAnswers {
                OAuthUserPrincipal(
                    tokenCredential = TokenCredential(googleToken, TokenType.OAUTH),
                    companyName = companyName,
                    name = name,
                    email = email,
                    organization = ROOT_ORG,
                    issuer = "google",
                )
            }

            // Act
            val response =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("x-issuer", "google")
                    header(HttpHeaders.Authorization, "Bearer $googleToken")
                }

            // Assert
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(
                ContentType.Application.Json,
                response.contentType(),
            )
            val responseBody = gson.fromJson(response.bodyAsText(), CreateOrganizationResponse::class.java)
            assertEquals(companyName, responseBody.organization.name)
            assertEquals(name, responseBody.organization.rootUser.name)
            assertEquals(email, responseBody.organization.rootUser.email)

            // Cleanup
            val orgId = responseBody.organization.id
            deleteOrganization(orgId)
        }
    }

    @Test
    fun `create organization using microsoft authorization - fail`() {
        testSuspend {
            // Arrange
            val microsoftToken = "test-microsoft-token"
            val name = "test-name"
            val email = "test-email"
            val companyName = "test-company"

            mockkObject(MicrosoftAuthProvider)
            coEvery {
                MicrosoftAuthProvider.getProfileDetails(any())
            } coAnswers {
                OAuthUserPrincipal(
                    tokenCredential = TokenCredential(microsoftToken, TokenType.OAUTH),
                    companyName = companyName,
                    name = name,
                    email = email,
                    organization = ROOT_ORG,
                    issuer = "microsoft",
                    metadata = AuthMetadata(id = UUID.randomUUID().toString()),
                )
            }

            // Act
            val response =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("x-issuer", "microsoft")
                    header(HttpHeaders.Authorization, "Bearer $microsoftToken")
                }

            // Assert
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `create organization - rollback on error`() {
        @Suppress("TooGenericExceptionThrown")
        declareMock<OrganizationRepo> {
            coEvery { this@declareMock.insert(any<Organizations>()) } coAnswers {
                throw Exception("Random DB error occurred")
            }
        }

        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test-name" + IdGenerator.randomId()
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
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateOrganizationResponse::class.java)
        }
    }

    @Test
    fun `get organization with invalid credentials`() {
        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val createOrganizationCall =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                RootUser(
                                    preferredUsername = preferredUsername,
                                    name = name,
                                    password = testPassword,
                                    email = testEmail,
                                    phone = testPhone,
                                ),
                            ),
                        ),
                    )
                }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.bodyAsText(), CreateOrganizationResponse::class.java)

            val response =
                testApp.client.get("/organizations/${createdOrganization.organization.id}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer test-bearer-token")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertFalse(response.headers.contains(HttpHeaders.ContentType))
            assertNull(response.headers[Constants.X_ORGANIZATION_HEADER])

            deleteOrganization(createdOrganization.organization.id)
        }
    }

    @Test
    fun `get organization success`() {
        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val createOrganizationCall =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                RootUser(
                                    preferredUsername = preferredUsername,
                                    name = name,
                                    password = testPassword,
                                    email = testEmail,
                                    phone = testPhone,
                                ),
                            ),
                        ),
                    )
                }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.bodyAsText(), CreateOrganizationResponse::class.java)

            val response =
                testApp.client.get("/organizations/${createdOrganization.organization.id}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            val fetchedOrganization = gson.fromJson(response.bodyAsText(), Organization::class.java)
            assertEquals(createdOrganization.organization, fetchedOrganization)

            deleteOrganization(createdOrganization.organization.id)
        }
    }

    @Test
    fun `get organization not found`() {
        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            // Create organization
            val createOrganizationCall =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                RootUser(
                                    preferredUsername = preferredUsername,
                                    name = name,
                                    password = testPassword,
                                    email = testEmail,
                                    phone = testPhone,
                                ),
                            ),
                        ),
                    )
                }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.bodyAsText(), CreateOrganizationResponse::class.java)

            val response =
                testApp.client.get("/organizations/inValidOrganizationId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}",
                    )
                }
            // These assertions
            assertEquals(HttpStatusCode.Forbidden, response.status)
            deleteOrganization(createdOrganization.organization.id)
        }
    }

    @Test
    fun `update organization name success`() {
        testSuspend {
            val orgName = "test-org" + IdGenerator.randomId()
            val orgDescription = "test-org-description"
            val preferredUsername = "user" + IdGenerator.randomId()
            val name = "test name"
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val createOrganizationCall =
                testApp.client.post("/organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-Api-Key", rootToken)
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                RootUser(
                                    preferredUsername = preferredUsername,
                                    name = name,
                                    password = testPassword,
                                    email = testEmail,
                                    phone = testPhone,
                                ),
                                orgDescription,
                            ),
                        ),
                    )
                }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.bodyAsText(), CreateOrganizationResponse::class.java)

            val updatedOrgName = "updated-org" + IdGenerator.randomId()
            val response =
                testApp.client.patch("/organizations/${createdOrganization.organization.id}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                    setBody(
                        gson.toJson(
                            UpdateOrganizationRequest(
                                updatedOrgName,
                            ),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            val fetchedOrganization = gson.fromJson(response.bodyAsText(), Organization::class.java)
            assertEquals(updatedOrgName, fetchedOrganization.name)
            assertEquals(orgDescription, fetchedOrganization.description)
        }
    }
}

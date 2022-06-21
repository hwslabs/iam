package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreateOrganizationResponse
import com.hypto.iam.server.models.Organization
import com.hypto.iam.server.models.RootUser
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.utils.IdGenerator
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
import io.mockk.verify
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.text.Charsets.UTF_8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.mock.declareMock
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest

@Testcontainers
internal class OrganizationApiKtTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `create organization with valid root credentials`() {

        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"
            val verified = true

            lateinit var orgId: String
            val requestBody = CreateOrganizationRequest(
                orgName,
                RootUser(userName, testPassword, testEmail, testPhone, verified)
            )
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            ) {
                val responseBody = gson.fromJson(response.content, CreateOrganizationResponse::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                orgId = responseBody.organization!!.id
                assertEquals(requestBody.name, responseBody.organization!!.name)
                assertEquals(10, responseBody.organization!!.id.length)
            }

            DataSetupHelper.deleteOrganization(orgId, this)
        }
    }

    @Test
    fun `create organization with invalid root credentials`() {
        declareMock<PasscodeRepo> {
            coEvery {
                getValidPasscode(
                    any<String>(),
                    any<VerifyEmailRequest.Purpose>(),
                    any<String>()
                )
            } returns null
        }
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", "bad creds")
                    setBody(
                        gson.toJson(
                            CreateOrganizationRequest(
                                orgName,
                                RootUser(userName, testPassword, testEmail, testPhone, true)
                            )
                        )
                    )
                }
            ) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertFalse(response.headers.contains(HttpHeaders.ContentType))
                assertEquals(null, response.content)
            }
        }
    }

    @Test
    fun `create organization with verify email method`() {

        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"
            val testPasscode = "testPasscode"
            val verified = true

            lateinit var orgId: String
            val verifyRequestBody = VerifyEmailRequest(
                email = testEmail,
                purpose = VerifyEmailRequest.Purpose.signup
            )
            handleRequest(HttpMethod.Post, "/verifyEmail") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(verifyRequestBody))
            }
            val requestBody = CreateOrganizationRequest(
                orgName,
                RootUser(userName, testPassword, testEmail, testPhone, verified)
            )
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", testPasscode)
                    setBody(gson.toJson(requestBody))
                }
            ) {
                val responseBody =
                    gson.fromJson(response.content, CreateOrganizationResponse::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(UTF_8),
                    response.contentType()
                )

                orgId = responseBody.organization!!.id
                assertEquals(requestBody.name, responseBody.organization!!.name)
                assertEquals(10, responseBody.organization!!.id.length)
            }

            DataSetupHelper.deleteOrganization(orgId, this)
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

        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val requestBody = CreateOrganizationRequest(
                orgName,
                RootUser(userName, testPassword, testEmail, testPhone, true)
            )
            with(
                handleRequest(HttpMethod.Post, "/organizations") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("X-Api-Key", rootToken)
                    setBody(gson.toJson(requestBody))
                }
            ) {
                val responseBody = gson.fromJson(response.content, CreateOrganizationResponse::class.java)
                verify {
                    cognitoClient.deleteUserPool(any<DeleteUserPoolRequest>())
                }
            }
        }
    }

    @Test
    fun `get organization with invalid credentials`() {
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            RootUser(userName, testPassword, testEmail, testPhone, true)
                        )
                    )
                )
            }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${createdOrganization.organization!!.id}") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer test-bearer-token")
                }
            ) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertFalse(response.headers.contains(HttpHeaders.ContentType))
                assertNull(response.headers[Constants.X_ORGANIZATION_HEADER])
                assertEquals(null, response.content)
            }

            DataSetupHelper.deleteOrganization(createdOrganization.organization!!.id, this)
        }
    }

    @Test
    fun `get organization success`() {
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            RootUser(userName, testPassword, testEmail, testPhone, true)
                        )
                    )
                )
            }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${createdOrganization.organization!!.id}") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                }

            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val fetchedOrganization = gson.fromJson(response.content, Organization::class.java)
                assertEquals(createdOrganization.organization, fetchedOrganization)
            }

            DataSetupHelper.deleteOrganization(createdOrganization.organization!!.id, this)
        }
    }

    @Test
    fun `get organization not found`() {
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            // Create organization
            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            RootUser(userName, testPassword, testEmail, testPhone, true)
                        )
                    )
                )
            }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            with(
                handleRequest(HttpMethod.Get, "/organizations/inValidOrganizationId") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${createdOrganization.rootUserToken}"
                    )
                }

            ) {
                // These assertions
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }

            DataSetupHelper.deleteOrganization(createdOrganization.organization!!.id, this)
        }
    }

    @Test
    fun `update organization name success`() {
        withTestApplication(Application::handleRequest) {
            val orgName = "test-org" + IdGenerator.randomId()
            val orgDescription = "test-org-description"
            val userName = "test-user" + IdGenerator.randomId()
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testPhone = "+919626012778"
            val testPassword = "testPassword@Hash1"

            val createOrganizationCall = handleRequest(HttpMethod.Post, "/organizations") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("X-Api-Key", rootToken)
                setBody(
                    gson.toJson(
                        CreateOrganizationRequest(
                            orgName,
                            RootUser(userName, testPassword, testEmail, testPhone, true),
                            orgDescription
                        )
                    )
                )
            }
            val createdOrganization =
                gson.fromJson(createOrganizationCall.response.content, CreateOrganizationResponse::class.java)

            val updatedOrgName = "updated-org" + IdGenerator.randomId()
            with(
                handleRequest(HttpMethod.Patch, "/organizations/${createdOrganization.organization!!.id}") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdOrganization.rootUserToken}")
                    setBody(
                        gson.toJson(
                            UpdateOrganizationRequest(
                                updatedOrgName
                            )
                        )
                    )
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val fetchedOrganization = gson.fromJson(response.content, Organization::class.java)
                assertEquals(updatedOrgName, fetchedOrganization.name)
                assertEquals(orgDescription, fetchedOrganization.description)
            }
        }
    }
}

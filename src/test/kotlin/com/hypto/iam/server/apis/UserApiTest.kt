package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.UserPaginatedResponse
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
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest

class UserApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `create user success case`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPassword@Hash1",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626012778",
            verified = true
        )
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!
            DataSetupHelper.createResource(organization.id, rootUserToken, this)

            with(
                handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(createUserRequest))
                }
            ) {
                val responseBody = gson.fromJson(response.content, User::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
                assertEquals(
                    organization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER]
                )

                assertEquals(createUserRequest.username, responseBody.username)
                assertEquals(createUserRequest.email, responseBody.email)
                assertEquals(User.Status.enabled.toString(), responseBody.status.toString())
                assertEquals(createUserRequest.verified, responseBody.verified)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get user request success case`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPassword@Hash1",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626012778"
        )
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            // Create user
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(createUserRequest))
            }

            // Get user
            with(handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users/testUserName") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }) {
                val responseBody = gson.fromJson(response.content, User::class.java)
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                assertEquals(createUserRequest.username, responseBody.username)
                assertEquals(false, responseBody.verified)
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get user with unauthorized access`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPasswordHash",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626012778"
        )
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            // Create user
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(createUserRequest))
            }

            // Get user
            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users/testUserName") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer badSecret")
                }
            ) {
                val responseBody = gson.fromJson(response.content, User::class.java)
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `delete user success case`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPassword@Hash1",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626012778"
        )
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            // Create user
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(createUserRequest))
            }

            // Delete user
            with(handleRequest(HttpMethod.Delete, "/organizations/${organization.id}/users/testUserName") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())

                // Verify the value from mocks
                val slot = slot<AdminDeleteUserRequest>()
                verify { get<CognitoIdentityProviderClient>().adminDeleteUser(capture(slot)) }
                val adminUserRequest = slot.captured
                assertEquals("testUserName", adminUserRequest.username())
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `delete root user`() {
        withTestApplication(Application::handleRequest) {
            val rootUserName = "test-root-user"
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this, rootUserName)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            // Delete root user
            with(handleRequest(HttpMethod.Delete,
                "/organizations/${organization.id}/users/$rootUserName") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `list users`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest1 = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPassword@Hash1",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626012778"
        )
        val createUserRequest2 = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPassword@Hash2",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626012778"
        )
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!
            DataSetupHelper.createResource(organization.id, rootUserToken, this)

            // Create user1
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(createUserRequest1))
            }

            // Create user2
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(createUserRequest2))
            }

            // List users
            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            ) {
                val slot = slot<ListUsersRequest>()
                verify { get<CognitoIdentityProviderClient>().listUsers(capture(slot)) }
                val listUserRequest = slot.captured
                assertEquals("testUserPoolId", listUserRequest.userPoolId())

                gson.fromJson(response.content, UserPaginatedResponse::class.java)
                assertEquals(HttpStatusCode.OK, response.status())
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `update user`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val userName = "testUserName"

            val createUserRequest = CreateUserRequest(
                username = "testUserName",
                passwordHash = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                verified = false
            )

            // Create user1
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(createUserRequest))
            }

            val updateUserRequest = UpdateUserRequest(
                phone = "+911234567890",
                status = UpdateUserRequest.Status.enabled,
                verified = true
            )

            with(
                handleRequest(
                    HttpMethod.Patch,
                    "/organizations/${organization.id}/users/$userName"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(updateUserRequest))
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `create user with validation error case`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "hypto.in"
        val createUserRequest = CreateUserRequest(
            username = "testUserName",
            passwordHash = "testPassword@ash",
            email = testEmail,
            status = CreateUserRequest.Status.enabled,
            phone = "+919626"
        )
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!
            DataSetupHelper.createResource(organization.id, rootUserToken, this)

            with(
                handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(createUserRequest))
                }
            ) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }
}

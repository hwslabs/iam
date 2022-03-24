package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.UserPaginatedResponse
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
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
        val createUserRequest = CreateUserRequest(username = "testUserName",
                                                    passwordHash = "testPasswordHash",
                                                    email = testEmail,
                                                    status = CreateUserRequest.Status.active,
                                                    phone = "+919626012778")
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!
            DataSetupHelper.createResource(organization.id, createdCredentials, this)

            with(
                handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    setBody(gson.toJson(createUserRequest))
                }
            ) {
                val responseBody = gson.fromJson(response.content, User::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                assertEquals(createUserRequest.username, responseBody.username)
                assertEquals(createUserRequest.email, responseBody.email)
                assertEquals(User.Status.enabled.toString(), responseBody.status.toString())
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get user request success case`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(username = "testUserName",
            passwordHash = "testPasswordHash",
            email = testEmail,
            status = CreateUserRequest.Status.active,
            phone = "+919626012778")
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            // Create user
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                setBody(gson.toJson(createUserRequest))
            }

            // Get user
            with(handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users/testUserName") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
            }) {
                val responseBody = gson.fromJson(response.content, User::class.java)
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                assertEquals(createUserRequest.username, responseBody.username)
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get user with unauthorized access`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(username = "testUserName",
            passwordHash = "testPasswordHash",
            email = testEmail,
            status = CreateUserRequest.Status.active,
            phone = "+919626012778")
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            // Create user
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                setBody(gson.toJson(createUserRequest))
            }

            // Get user
            with(handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users/testUserName") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer badSecret")
            }) {
                val responseBody = gson.fromJson(response.content, User::class.java)
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `delete user success case`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest = CreateUserRequest(username = "testUserName",
            passwordHash = "testPasswordHash",
            email = testEmail,
            status = CreateUserRequest.Status.active,
            phone = "+919626012778")
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            // Create user
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                setBody(gson.toJson(createUserRequest))
            }

            // Delete user
            with(handleRequest(HttpMethod.Delete, "/organizations/${organization.id}/users/testUserName") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
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
    fun `list users`() {
        val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
        val createUserRequest1 = CreateUserRequest(username = "testUserName",
            passwordHash = "testPasswordHash",
            email = testEmail,
            status = CreateUserRequest.Status.active,
            phone = "+919626012778")
        val createUserRequest2 = CreateUserRequest(username = "testUserName",
            passwordHash = "testPasswordHash",
            email = testEmail,
            status = CreateUserRequest.Status.active,
            phone = "+919626012778")
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!
            DataSetupHelper.createResource(organization.id, createdCredentials, this)

            // Create user1
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                setBody(gson.toJson(createUserRequest1))
            }

            // Create user2
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                setBody(gson.toJson(createUserRequest2))
            }

            // List users
            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
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
            val createdCredentials = organizationResponse.adminUserCredential!!
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"

            val createUserRequest = CreateUserRequest(username = "testUserName",
                passwordHash = "testPasswordHash",
                email = testEmail,
                status = CreateUserRequest.Status.active,
                phone = "+919626012778")

            // Create user1
            handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                setBody(gson.toJson(createUserRequest))
            }

            with(
                handleRequest(
                    HttpMethod.Patch,
                    "/organizations/${organization.id}/users/${createUserRequest.username}"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(UpdateUserRequest(email = "updatedEmail@email.com", phone = "+1234567890")))
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
}

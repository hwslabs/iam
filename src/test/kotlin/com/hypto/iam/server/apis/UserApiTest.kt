package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.idp.CognitoConstants
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.ChangeUserPasswordRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.ResetPasswordRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.UserPaginatedResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.utils.IamResources
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
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.util.Base64
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import org.koin.test.inject
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

class UserApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Nested
    @DisplayName("Create user API tests")
    inner class CreateUserTest {
        @Test
        fun `create user success case`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest = CreateUserRequest(
                preferredUsername = "testUserName",
                name = "lorem ipsum",
                password = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                verified = true,
                loginAccess = true
            )
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns AdminGetUserResponse.builder()
                    .enabled(true)
                    .username(organization.rootUser.username)
                    .userAttributes(
                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                            .value(organization.rootUser.email).build(),
                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                            .value(organization.rootUser.phone).build()
                    )
                    .userCreateDate(Instant.now())
                    .build()

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

                    assertEquals(createUserRequest.preferredUsername, responseBody.preferredUsername)
                    assertEquals(createUserRequest.email, responseBody.email)
                    assertEquals(User.Status.enabled.toString(), responseBody.status.toString())
                    assertEquals(createUserRequest.verified, responseBody.verified)
                }

                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }

        @Test
        fun `create user without preferred username`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest = CreateUserRequest(
                name = "lorem ipsum",
                password = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                verified = true,
                loginAccess = true
            )
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns AdminGetUserResponse.builder()
                    .enabled(true)
                    .username(organization.rootUser.username)
                    .userAttributes(
                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                            .value(organization.rootUser.email).build(),
                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                            .value(organization.rootUser.phone).build()
                    )
                    .userCreateDate(Instant.now())
                    .build()

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

                    assertEquals(null, responseBody.preferredUsername)
                    assertEquals(createUserRequest.email, responseBody.email)
                    assertEquals(User.Status.enabled.toString(), responseBody.status.toString())
                    assertEquals(createUserRequest.verified, responseBody.verified)
                }

                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }

        @Test
        fun `create user with same preferred username - failure`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val preferredUsername = "testUserName"
            val createUserRequest = CreateUserRequest(
                name = "lorem ipsum",
                preferredUsername = preferredUsername,
                password = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                verified = true,
                loginAccess = true
            )
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this, preferredUsername)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                coEvery {
                    cognitoClient.adminCreateUser(any<AdminCreateUserRequest>())
                } throws UsernameExistsException.builder()
                    .build()
                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } throws UserNotFoundException.builder()
                    .build()

                DataSetupHelper.createResource(organization.id, rootUserToken, this)

                with(
                    handleRequest(HttpMethod.Post, "/organizations/${organization.id}/users") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(createUserRequest))
                    }
                ) {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
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
                preferredUsername = "testUserName",
                name = "lorem ipsum",
                password = "testPassword@ash",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919999999999",
                loginAccess = true
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

    @Nested
    @DisplayName("Get user API tests")
    inner class GetUserTest {
        @Test
        fun `get user request success case`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest = CreateUserRequest(
                preferredUsername = "testUserName",
                name = "lorem ipsum",
                password = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                loginAccess = true
            )
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                // Create user
                val (user1, _) = DataSetupHelper.createUser(
                    orgId = organization.id,
                    bearerToken = rootUserToken,
                    createUserRequest = createUserRequest,
                    this
                )

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns AdminGetUserResponse.builder()
                    .enabled(true)
                    .username(user1.username)
                    .userAttributes(
                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                            .value(user1.email).build(),
                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                            .value(user1.preferredUsername).build()
                    )
                    .userCreateDate(Instant.now())
                    .build()

                // Get user
                with(
                    handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users/${user1.username}") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    val responseBody = gson.fromJson(response.content, User::class.java)
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )

                    assertEquals(createUserRequest.preferredUsername, responseBody.preferredUsername)
                    assertEquals(false, responseBody.verified)
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }

        @Test
        fun `get user with unauthorized access`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest = CreateUserRequest(
                preferredUsername = "testUserName",
                name = "lorem ipsum",
                password = "testPassword@123",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                loginAccess = true
            )
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                // Create user
                val (user1, _) = DataSetupHelper.createUser(
                    orgId = organization.id,
                    bearerToken = rootUserToken,
                    createUserRequest = createUserRequest,
                    this
                )

                // Get user
                with(
                    handleRequest(HttpMethod.Get, "/organizations/${organization.id}/users/${user1.username}") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer badSecret")
                    }
                ) {
                    assertEquals(HttpStatusCode.Unauthorized, response.status())
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }
    }

    @Nested
    @DisplayName("delete user API tests")
    inner class DeleteUserTest {
        @Test
        fun `delete user success case`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest = CreateUserRequest(
                preferredUsername = "testUserName",
                name = "lorem ipsum",
                password = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                loginAccess = true
            )
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                // Create user
                val (user1, _) = DataSetupHelper.createUser(
                    orgId = organization.id,
                    bearerToken = rootUserToken,
                    createUserRequest = createUserRequest,
                    this
                )

                // Delete user
                with(
                    handleRequest(HttpMethod.Delete, "/organizations/${organization.id}/users/${user1.username}") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    assertEquals(HttpStatusCode.OK, response.status())

                    // Verify the value from mocks
                    val slot = slot<AdminDeleteUserRequest>()
                    verify { get<CognitoIdentityProviderClient>().adminDeleteUser(capture(slot)) }
                    val adminUserRequest = slot.captured
                    assertEquals(user1.username, adminUserRequest.username())
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }

        @Test
        fun `delete root user`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken

                val username = organizationResponse.organization.rootUser.username

                // Delete root user
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${organization.id}/users/$username"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }
    }

    @Nested
    @DisplayName("List user API tests")
    inner class ListUserTest {
        @Test
        fun `list users`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest1 = CreateUserRequest(
                preferredUsername = "testUserName",
                name = "lorem ipsum",
                password = "testPassword@Hash1",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                loginAccess = true
            )
            val createUserRequest2 = CreateUserRequest(
                preferredUsername = "testUserName2",
                name = "lorem ipsum",
                password = "testPassword@Hash2",
                email = testEmail,
                status = CreateUserRequest.Status.enabled,
                phone = "+919626012778",
                loginAccess = true
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
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

                    val responseBody = gson.fromJson(response.content, UserPaginatedResponse::class.java)
                    assertEquals(responseBody.data!!.size, 2)
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }
    }

    @Nested
    @DisplayName("Update user API tests")
    inner class UpdateUserTest {
        @Test
        fun `update user`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!
                val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"

                val createUserRequest = CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = testEmail,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    verified = false,
                    loginAccess = true
                )

                // Create user1
                val (user1, _) = DataSetupHelper.createUser(
                    orgId = organization.id,
                    bearerToken = rootUserToken,
                    createUserRequest = createUserRequest,
                    this
                )

                val updateUserRequest = UpdateUserRequest(
                    name = "name updated",
                    phone = "+911234567890",
                    status = UpdateUserRequest.Status.enabled,
                    verified = true
                )

                with(
                    handleRequest(
                        HttpMethod.Patch,
                        "/organizations/${organization.id}/users/${user1.username}"
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
    }

    @Nested
    @DisplayName("Change user password API tests")
    inner class ChangeUserPasswordTest {

        @Test
        fun `change root user password - success`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, rootUser) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!
                val username = organizationResponse.organization.rootUser.username

                val changePasswordRequest = ChangeUserPasswordRequest(
                    oldPassword = "testPassword@Hash1",
                    newPassword = "testPassword@Hash2"
                )
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${organization.id}/users/$username/change_password"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
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
        fun `change password with wrong old password - failure`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, rootUser) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!
                val username = organizationResponse.organization.rootUser.username

                coEvery {
                    cognitoClient.adminInitiateAuth(any<AdminInitiateAuthRequest>())
                } throws NotAuthorizedException.builder().message("Incorrect username or password").build()

                val changePasswordRequest = ChangeUserPasswordRequest(
                    oldPassword = "testPassword@Hash3",
                    newPassword = "testPassword@Hash2"
                )
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${organization.id}/users/$username/change_password"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                ) {
                    assertEquals(HttpStatusCode.Unauthorized, response.status())
                    assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }

        @Test
        fun `user to change password on their own with permission - success`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                val createUser1Request = CreateUserRequest(
                    preferredUsername = "testUserName1",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = "test-user-email1" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    verified = true,
                    loginAccess = true
                )

                val (user1, credential) = DataSetupHelper.createUser(
                    organization.id,
                    rootUserToken,
                    createUser1Request,
                    this
                )

                DataSetupHelper.createAndAttachPolicy(
                    orgId = organization.id,
                    username = user1.username,
                    bearerToken = rootUserToken,
                    policyName = "user1-policy",
                    accountId = null,
                    resourceName = IamResources.USER,
                    actionName = "changePassword",
                    resourceInstance = user1.username,
                    engine = this
                )

                val changePasswordRequest = ChangeUserPasswordRequest(
                    oldPassword = "testPassword@Hash1",
                    newPassword = "testPassword@Hash2"
                )
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${organization.id}/users/${user1.username}/change_password"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer ${credential.secret}")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
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
        fun `change password of different user without permission - failure`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                val createUser1Request = CreateUserRequest(
                    preferredUsername = "testUserName1",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = "test-user-email1" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    verified = true,
                    loginAccess = true
                )

                val createUser2Request = CreateUserRequest(
                    preferredUsername = "testUserName2",
                    name = "lorem ipsum",
                    password = "testPassword@Hash2",
                    email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    verified = true,
                    loginAccess = true
                )
                val (user1, _) =
                    DataSetupHelper.createUser(
                        organization.id,
                        rootUserToken,
                        createUser1Request,
                        this
                    )
                val (user2, credential) = DataSetupHelper.createUser(
                    organization.id,
                    rootUserToken,
                    createUser2Request,
                    this
                )

                DataSetupHelper.createAndAttachPolicy(
                    orgId = organization.id,
                    username = user2.username,
                    bearerToken = rootUserToken,
                    policyName = "user2-policy",
                    accountId = null,
                    resourceName = IamResources.USER,
                    actionName = "changePassword",
                    resourceInstance = user2.username,
                    engine = this
                )

                val changePasswordRequest = ChangeUserPasswordRequest(
                    oldPassword = "testPassword@Hash1",
                    newPassword = "testPassword@Hash2"
                )
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${organization.id}/users/${user1.username}/change_password"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer ${credential.secret}")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                ) {
                    assertEquals(HttpStatusCode.Forbidden, response.status())
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }

        @Test
        fun `generate token after changing password - success`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, rootUser) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!
                val username = organizationResponse.organization.rootUser.username

                val changePasswordRequest = ChangeUserPasswordRequest(
                    oldPassword = "testPassword@Hash1",
                    newPassword = "testPassword@Hash2"
                )
                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${organization.id}/users/$username/change_password"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                ) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }

                val authString = "${rootUser.email}:${changePasswordRequest.newPassword}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${organization.id}/token"
                    ) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(
                            HttpHeaders.Authorization,
                            authHeader
                        )
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

    @Nested
    @DisplayName("Reset user password API tests")
    inner class ResetUserPasswordTest {
        @Test
        fun `reset Password - success`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, createdUser) = DataSetupHelper.createOrganization(this)
                val organizationId = organizationResponse.organization!!.id
                val testPasscode = "testPasscode"

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
                                            CognitoConstants.ATTRIBUTE_PREFIX_CUSTOM +
                                                CognitoConstants.ATTRIBUTE_CREATED_BY
                                        ).value("iam-system").build()
                                    )
                                ).userCreateDate(Instant.now()).build()
                        )
                    ).build()
                coEvery {
                    cognitoClient.listUsers(any<ListUsersRequest>())
                } returns listUsersResponse

                handleRequest(HttpMethod.Post, "/verifyEmail") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        gson.toJson(
                            VerifyEmailRequest(
                                email = createdUser.email,
                                purpose = VerifyEmailRequest.Purpose.reset,
                                organizationId = organizationId
                            )
                        )
                    )
                }

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/$organizationId/users/resetPassword"
                    ) {
                        addHeader("X-Api-Key", testPasscode)
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            gson.toJson(
                                ResetPasswordRequest(email = createdUser.email, password = "testPassword@123")
                            )
                        )
                    }
                ) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    val response = gson.fromJson(response.content, BaseSuccessResponse::class.java)
                    assertTrue(response.success)
                }

                DataSetupHelper.deleteOrganization(organizationId, this)
            }
        }
    }

    @Nested
    @DisplayName("Attach policy to user API tests")
    inner class UserAttachPolicyTest {
        @Test
        fun `user to attach policy to a different user with permission - success`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                val createUser1Request = CreateUserRequest(
                    preferredUsername = "testUserName1",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919999999999",
                    verified = true,
                    loginAccess = true
                )

                val (user1, _) = DataSetupHelper.createUser(
                    organization.id,
                    rootUserToken,
                    createUser1Request,
                    this
                )
                val createUser2Request = CreateUserRequest(
                    preferredUsername = "testUserName2",
                    name = "lorem ipsum",
                    password = "testPassword@Hash2",
                    email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919999999999",
                    verified = true,
                    loginAccess = true
                )
                val (user2, credential) = DataSetupHelper.createUser(
                    organization.id,
                    rootUserToken,
                    createUser2Request,
                    this
                )

                DataSetupHelper.createAndAttachPolicy(
                    orgId = organization.id,
                    username = user2.username,
                    bearerToken = rootUserToken,
                    policyName = "test-policy",
                    accountId = null,
                    resourceName = IamResources.USER,
                    actionName = "attachPolicies",
                    resourceInstance = user1.username,
                    engine = this
                )

                val samplePolicy = DataSetupHelper.createAndAttachPolicy(
                    orgId = organization.id,
                    username = null,
                    bearerToken = rootUserToken,
                    policyName = "sample-policy",
                    accountId = null,
                    resourceName = "sample-resource",
                    actionName = "sample-action",
                    resourceInstance = "instanceId",
                    engine = this
                )
                with(
                    handleRequest(
                        HttpMethod.Patch,
                        "/organizations/${organization.id}/users/${user1.username}/attach_policies"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer ${credential.secret}")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(PolicyAssociationRequest(listOf(samplePolicy.hrn))))
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
        fun `user to attach policies to a different user without permission - failure`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
                val organization = organizationResponse.organization!!
                val rootUserToken = organizationResponse.rootUserToken!!

                val createUser1Request = CreateUserRequest(
                    preferredUsername = "testUserName1",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919999999999",
                    verified = true,
                    loginAccess = true
                )

                val (user1, _) = DataSetupHelper.createUser(
                    organization.id,
                    rootUserToken,
                    createUser1Request,
                    this
                )
                val createUser2Request = CreateUserRequest(
                    preferredUsername = "testUserName2",
                    name = "lorem ipsum",
                    password = "testPassword@Hash2",
                    email = "test-user-email" + IdGenerator.randomId() + "@iam.in",
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919999999999",
                    verified = true,
                    loginAccess = true
                )
                val (user2, credential) = DataSetupHelper.createUser(
                    organization.id,
                    rootUserToken,
                    createUser2Request,
                    this
                )
                DataSetupHelper.createAndAttachPolicy(
                    orgId = organization.id,
                    username = user2.username,
                    bearerToken = rootUserToken,
                    policyName = "test-policy",
                    accountId = null,
                    resourceName = IamResources.USER,
                    actionName = "attachPolicies",
                    resourceInstance = user2.username,
                    engine = this
                )

                val samplePolicy = DataSetupHelper.createAndAttachPolicy(
                    orgId = organization.id,
                    username = null,
                    bearerToken = rootUserToken,
                    policyName = "sample-policy",
                    accountId = null,
                    resourceName = "sample-resource",
                    actionName = "sample-action",
                    resourceInstance = "instanceId",
                    engine = this
                )

                with(
                    handleRequest(
                        HttpMethod.Patch,
                        "/organizations/${organization.id}/users/${user1.username}/attach_policies"
                    ) {
                        addHeader(HttpHeaders.Authorization, "Bearer ${credential.secret}")
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(PolicyAssociationRequest(listOf(samplePolicy.hrn))))
                    }
                ) {
                    assertEquals(HttpStatusCode.Forbidden, response.status())
                    assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                }
                DataSetupHelper.deleteOrganization(organization.id, this)
            }
        }
    }
}

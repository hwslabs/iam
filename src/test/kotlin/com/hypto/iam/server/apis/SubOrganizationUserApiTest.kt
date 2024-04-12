package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV3.createAndAttachPolicy
import com.hypto.iam.server.helpers.DataSetupHelperV3.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV3.createPolicy
import com.hypto.iam.server.helpers.DataSetupHelperV3.createResource
import com.hypto.iam.server.helpers.DataSetupHelperV3.createSubOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV3.createSubOrganizationUser
import com.hypto.iam.server.helpers.DataSetupHelperV3.createUser
import com.hypto.iam.server.helpers.DataSetupHelperV3.deleteOrganization
import com.hypto.iam.server.idp.CognitoConstants
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.ChangeUserPasswordRequest
import com.hypto.iam.server.models.CreateUserPasswordRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.CreateUserResponse
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.ResetPasswordRequest
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.UserPaginatedResponse
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.IdGenerator
import io.ktor.client.request.delete
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
import io.ktor.server.testing.TestApplication
import io.ktor.test.dispatcher.testSuspend
import io.mockk.coEvery
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import org.koin.test.inject
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType
import java.time.Instant
import java.util.Base64
import kotlin.test.assertTrue

class SubOrganizationUserApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    companion object {
        lateinit var testApp: TestApplication

        @JvmStatic
        @BeforeAll
        fun setupTest() {
            testApp =
                TestApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                }
        }

        @JvmStatic
        @AfterAll
        fun teardownTest() {
            testApp.stop()
        }
    }

    @Nested
    @DisplayName("Create sub organization user API tests")
    inner class CreateSubOrganizationUserTest {
        @Test
        fun `create user success case`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest =
                CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = testEmail,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    verified = true,
                    loginAccess = true,
                )
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns
                    AdminGetUserResponse.builder()
                        .enabled(true)
                        .username(organization.rootUser.username)
                        .userAttributes(
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                .value(organization.rootUser.email).build(),
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                                .value(organization.rootUser.phone).build(),
                        )
                        .userCreateDate(Instant.now())
                        .build()

                testApp.createResource(organization.id, rootUserToken)

                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(createUserRequest))
                    }
                val responseBody = gson.fromJson(response.bodyAsText(), CreateUserResponse::class.java)
                assertEquals(HttpStatusCode.Created, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                assertEquals(
                    organization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER],
                )

                assertEquals(createUserRequest.preferredUsername, responseBody.user.preferredUsername)
                assertEquals(createUserRequest.email, responseBody.user.email)
                assertEquals(User.Status.enabled.toString(), responseBody.user.status.toString())
                assertEquals(createUserRequest.verified, responseBody.user.verified)

                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `create user without preferred username`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest =
                CreateUserRequest(
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = testEmail,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    verified = true,
                    loginAccess = true,
                )
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns
                    AdminGetUserResponse.builder()
                        .enabled(true)
                        .username(organization.rootUser.username)
                        .userAttributes(
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                .value(organization.rootUser.email).build(),
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                                .value(organization.rootUser.phone).build(),
                        )
                        .userCreateDate(Instant.now())
                        .build()

                testApp.createResource(organization.id, rootUserToken)

                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(createUserRequest))
                    }
                val responseBody = gson.fromJson(response.bodyAsText(), CreateUserResponse::class.java)
                assertEquals(HttpStatusCode.Created, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                assertEquals(
                    organization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER],
                )

                assertEquals(null, responseBody.user.preferredUsername)
                assertEquals(createUserRequest.email, responseBody.user.email)
                assertEquals(User.Status.enabled.toString(), responseBody.user.status.toString())
                assertEquals(createUserRequest.verified, responseBody.user.verified)

                testApp.deleteOrganization(organization.id)
            }
        }

//        @Test
//        fun `create user with same preferred username - failure`() {
//            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
//            val preferredUsername = "testUserName"
//            val createUserRequest = CreateUserRequest(
//                name = "lorem ipsum",
//                preferredUsername = preferredUsername,
//                password = "testPassword@Hash1",
//                email = testEmail,
//                status = CreateUserRequest.Status.enabled,
//                phone = "+919626012778",
//                verified = true,
//                loginAccess = true
//            )
//            testApplication {
//                environment {
//                    config = ApplicationConfig("application-custom.conf")
//                }
//                val (organizationResponse, _) = testApp.createOrganization(preferredUsername)
//                val organization = organizationResponse.organization
//                val rootUserToken = organizationResponse.rootUserToken
//                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
//
//                coEvery {
//                    cognitoClient.adminCreateUser(any<AdminCreateUserRequest>())
//                } throws UsernameExistsException.builder()
//                    .build()
//                coEvery {
//                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
//                } throws UserNotFoundException.builder()
//                    .build()
//
//                testApp.createResource(organization.id, rootUserToken)
//
//                val response = testApp.client.post(
//                    "/organizations/${organization
//                        .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users"
//                ) {
//                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
//                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
//                    setBody(gson.toJson(createUserRequest))
//                }
//                assertEquals(HttpStatusCode.BadRequest, response.status)
//                assertEquals(
//                    ContentType.Application.Json,
//                    response.contentType()
//                )
//
//                testApp.deleteOrganization(organization.id)
//            }
//        }

        @Test
        fun `create user with validation error case`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "hypto.in"
            val createUserRequest =
                CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    password = "testPassword@ash",
                    email = testEmail,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919999999999",
                    loginAccess = true,
                )
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                testApp.createResource(organization.id, rootUserToken)
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)

                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(createUserRequest))
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `create user without login access - success case`() {
            val createUserRequest =
                CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    email = "test-user-email" + IdGenerator.randomId() + "@hypto.in",
                    status = CreateUserRequest.Status.enabled,
                    verified = true,
                    loginAccess = false,
                )
            testSuspend {
                val (organizationResponse, rootUser) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)

                testApp.createResource(organization.id, rootUserToken)

                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(createUserRequest))
                    }
                val responseBody = gson.fromJson(response.bodyAsText(), CreateUserResponse::class.java)
                assertEquals(HttpStatusCode.Created, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                assertEquals(
                    organization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER],
                )

                assertEquals(createUserRequest.preferredUsername, responseBody.user.preferredUsername)
                assertEquals(createUserRequest.email, responseBody.user.email)
                assertEquals(createUserRequest.name, responseBody.user.name)
                assertEquals(User.Status.enabled.toString(), responseBody.user.status.toString())
                assertEquals(createUserRequest.verified, responseBody.user.verified)
                assertEquals(createUserRequest.loginAccess, responseBody.user.loginAccess)

                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `create user without login access - request validation error`() {
            val createUserRequest =
                CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    status = CreateUserRequest.Status.enabled,
                    verified = true,
                    email = "testuser@test.com",
                    password = "testPassword@ash",
                    loginAccess = false,
                )
            testSuspend {
                val (organizationResponse, rootUser) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)

                testApp.createResource(organization.id, rootUserToken)

                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(createUserRequest))
                    }
                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("Get user API tests")
    inner class GetUserTest {
        @Test
        fun `get user request success case`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, subOrgUserCreds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns
                    AdminGetUserResponse.builder()
                        .enabled(true)
                        .username(subOrgUser.username)
                        .userAttributes(
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                .value(subOrgUser.email).build(),
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                                .value(subOrgUser.preferredUsername).build(),
                        )
                        .userCreateDate(Instant.now())
                        .build()

                // Get user
                val response =
                    testApp.client.get(
                        "/organizations/${organization.id}" +
                            "/sub_organizations/${subOrganizationResponse.subOrganization.name}" +
                            "/users/${subOrgUser.username}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                val responseBody = gson.fromJson(response.bodyAsText(), User::class.java)
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                assertEquals(subOrgUser.preferredUsername, responseBody.preferredUsername)
                assertEquals(true, responseBody.verified)
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `get user with unauthorized access`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest =
                CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    password = "testPassword@123",
                    email = testEmail,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    loginAccess = true,
                )
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                // Create user
                val (user1, _) =
                    testApp.createUser(
                        orgId = organization.id,
                        bearerToken = rootUserToken,
                        createUserRequest = createUserRequest,
                    )

                // Get user
                val response =
                    testApp.client.get(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users/${user1.username}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer badSecret")
                    }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("delete user API tests")
    inner class DeleteUserTest {
        @Test
        fun `delete user success case`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, _) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                // Delete user
                val response =
                    testApp.client.delete(
                        "/organizations/${organization.id}" +
                            "/sub_organizations/${subOrganizationResponse.subOrganization.name}" +
                            "/users/${subOrgUser.username}",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                assertEquals(HttpStatusCode.OK, response.status)

                // Verify the value from mocks
                val slot = slot<AdminDeleteUserRequest>()
                verify { get<CognitoIdentityProviderClient>().adminDeleteUser(capture(slot)) }
                val adminUserRequest = slot.captured
                assertEquals(subOrgUser.username, adminUserRequest.username())
                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("List user API tests")
    inner class ListUserTest {
        @Test
        fun `list users`() {
            val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"
            val testEmail1 = "test-user-email1" + IdGenerator.randomId() + "@hypto.in"
            val createUserRequest1 =
                CreateUserRequest(
                    preferredUsername = "testUserName",
                    name = "lorem ipsum",
                    password = "testPassword@Hash1",
                    email = testEmail,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    loginAccess = true,
                )
            val createUserRequest2 =
                CreateUserRequest(
                    preferredUsername = "testUserName2",
                    name = "lorem ipsum",
                    password = "testPassword@Hash2",
                    email = testEmail1,
                    status = CreateUserRequest.Status.enabled,
                    phone = "+919626012778",
                    loginAccess = true,
                )
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                testApp.createResource(organization.id, rootUserToken)

                // Create user1
                testApp.client.post(
                    "/organizations/${organization
                        .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(createUserRequest1))
                }

                // Create user2
                testApp.client.post(
                    "/organizations/${organization
                        .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(createUserRequest2))
                }

                // List users
                val listUsersResponse =
                    testApp.client.get(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization.name}/users",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                assertEquals(HttpStatusCode.OK, listUsersResponse.status)
                assertEquals(
                    ContentType.Application.Json,
                    listUsersResponse.contentType(),
                )

                val responseBody = gson.fromJson(listUsersResponse.bodyAsText(), UserPaginatedResponse::class.java)
                assertEquals(responseBody.data!!.size, 2)
                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("Update user API tests")
    inner class UpdateUserTest {
        @Test
        fun `update user`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val testEmail = "test-user-email" + IdGenerator.randomId() + "@hypto.in"

                // Create sub org user
                val (subOrgUser, _) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                val updateUserRequest =
                    UpdateUserRequest(
                        name = "name updated",
                        phone = "+911234567890",
                        status = UpdateUserRequest.Status.enabled,
                        verified = true,
                    )

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns
                    AdminGetUserResponse.builder()
                        .enabled(true)
                        .username(subOrgUser.username)
                        .userAttributes(
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                .value(subOrgUser.email).build(),
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                                .value(subOrgUser.username).build(),
                        )
                        .userCreateDate(Instant.now())
                        .build()

                val response =
                    testApp.client.patch(
                        "/organizations/${organization.id}" +
                            "/sub_organizations/${subOrganizationResponse.subOrganization.name}" +
                            "/users/${subOrgUser.username}",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(updateUserRequest))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("Change user password API tests")
    inner class ChangeUserPasswordTest {
        @Test
        fun `change password with wrong old password - failure`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, subOrgUserCreds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )
                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = subOrgUser.username,
                    bearerToken = rootUserToken,
                    policyName = "sample-policy2",
                    subOrgName = subOrganizationResponse.subOrganization.name,
                    resourceName = "iam-user",
                    actionName = "changePassword",
                    resourceInstance = subOrgUser.username,
                )

                coEvery {
                    cognitoClient.adminInitiateAuth(any<AdminInitiateAuthRequest>())
                } throws NotAuthorizedException.builder().message("Incorrect username or password").build()

                val changePasswordRequest =
                    ChangeUserPasswordRequest(
                        oldPassword = "testPassword@Hash3",
                        newPassword = "testPassword@Hash2",
                    )
                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/${subOrgUser.username}/change_password",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${subOrgUserCreds.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `user to change password on their own with permission - success`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, subOrgUserCred) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = subOrgUser.username,
                    bearerToken = rootUserToken,
                    policyName = "user1-policy",
                    subOrgName = subOrganizationResponse.subOrganization.name,
                    resourceName = IamResources.USER,
                    actionName = "changePassword",
                    resourceInstance = subOrgUser.username,
                )

                val changePasswordRequest =
                    ChangeUserPasswordRequest(
                        oldPassword = "testPassword@Hash1",
                        newPassword = "testPassword@Hash2",
                    )
                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/${subOrgUser.username}/change_password",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${subOrgUserCred.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `change password of different user without permission - failure`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser1, subOrgUser1Creds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )
                val (subOrgUser2, subOrgUser2Creds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = subOrgUser2.username,
                    bearerToken = subOrgUser2Creds.secret,
                    policyName = "user2-policy",
                    subOrgName = subOrganizationResponse.subOrganization.name,
                    resourceName = IamResources.USER,
                    actionName = "changePassword",
                    resourceInstance = subOrgUser2.username,
                )

                val changePasswordRequest =
                    ChangeUserPasswordRequest(
                        oldPassword = "testPassword@Hash1",
                        newPassword = "testPassword@Hash2",
                    )
                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/${subOrgUser1.username}/change_password",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${subOrgUser2Creds.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                assertEquals(HttpStatusCode.Forbidden, response.status)
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `generate token after changing password - success`() {
            testSuspend {
                val (organizationResponse, rootUser) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, subOrgUserCreds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )
                val samplePolicy =
                    testApp.createAndAttachPolicy(
                        orgId = organization.id,
                        username = subOrgUser.username,
                        bearerToken = rootUserToken,
                        policyName = "sample-policy3",
                        subOrgName = subOrganizationResponse.subOrganization.name,
                        resourceName = "iam-user",
                        actionName = "changePassword",
                        resourceInstance = subOrgUser.username,
                    )

                val username = organizationResponse.organization.rootUser.username

                val changePasswordRequest =
                    ChangeUserPasswordRequest(
                        oldPassword = "testPassword@Hash1",
                        newPassword = "testPassword@Hash2",
                    )
                val response =
                    testApp.client.post(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/${subOrgUser.username}/change_password",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${subOrgUserCreds.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(changePasswordRequest))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                val authString = "${subOrgUser.email}:${changePasswordRequest.newPassword}"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                val tokenResponse =
                    testApp.client.post(
                        "/organizations/${organization.id}/sub_organizations/token",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            authHeader,
                        )
                    }

                assertEquals(HttpStatusCode.OK, tokenResponse.status)
                assertEquals(
                    ContentType.Application.Json,
                    tokenResponse.contentType(),
                )
                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("Reset user password API tests")
    inner class ResetUserPasswordTest {
        @Test
        fun `reset Password - success`() {
            testSuspend {
                val (organizationResponse, createdUser) = testApp.createOrganization()
                val organizationId = organizationResponse.organization.id
                val subOrganizationResponse = testApp.createSubOrganization(organizationId, organizationResponse.rootUserToken)
                val (subOrgUser, _) =
                    testApp.createSubOrganizationUser(
                        organizationResponse.organization.id,
                        subOrganizationResponse.subOrganization.name,
                        organizationResponse.rootUserToken,
                        cognitoClient,
                    )
                val testPasscode = "testPasscode"

                val listUsersResponse =
                    ListUsersResponse.builder().users(
                        listOf(
                            UserType.builder().username(subOrgUser.username)
                                .enabled(true).attributes(
                                    listOf(
                                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_NAME)
                                            .value("test name")
                                            .build(),
                                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                            .value(subOrgUser.email)
                                            .build(),
                                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PHONE)
                                            .value(subOrgUser.phone)
                                            .build(),
                                        AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL_VERIFIED)
                                            .value("true")
                                            .build(),
                                        AttributeType.builder().name(
                                            CognitoConstants.ATTRIBUTE_PREFIX_CUSTOM +
                                                CognitoConstants.ATTRIBUTE_CREATED_BY,
                                        ).value("iam-system").build(),
                                    ),
                                ).userCreateDate(Instant.now()).build(),
                        ),
                    ).build()
                coEvery {
                    cognitoClient.listUsers(any<ListUsersRequest>())
                } returns listUsersResponse

                testApp.client.post("/verifyEmail") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        gson.toJson(
                            VerifyEmailRequest(
                                email = createdUser.email,
                                purpose = VerifyEmailRequest.Purpose.reset,
                                organizationId = organizationId,
                                subOrganizationName = subOrganizationResponse.subOrganization.name,
                            ),
                        ),
                    )
                }

                val resetPasswordResponse =
                    testApp.client.post(
                        "/organizations/$organizationId/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/resetPassword",
                    ) {
                        header("X-Api-Key", testPasscode)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            gson.toJson(
                                ResetPasswordRequest(email = subOrgUser.email!!, password = "testPassword@123"),
                            ),
                        )
                    }
                assertEquals(HttpStatusCode.OK, resetPasswordResponse.status)
                assertEquals(
                    ContentType.Application.Json,
                    resetPasswordResponse.contentType(),
                )
                val response = gson.fromJson(resetPasswordResponse.bodyAsText(), BaseSuccessResponse::class.java)
                assertTrue(response.success)

                testApp.deleteOrganization(organizationId)
            }
        }
    }

    @Nested
    @DisplayName("Attach policy to user API tests")
    inner class UserAttachPolicyTest {
        @Test
        fun `user to attach policy to a different user with permission - success`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, subOrgUserCreds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = subOrgUser.username,
                    bearerToken = rootUserToken,
                    policyName = "test-policy",
                    subOrgName = subOrganizationResponse.subOrganization.name,
                    resourceName = IamResources.USER,
                    actionName = "attachPolicies",
                    resourceInstance = subOrgUser.username,
                )

                val samplePolicy =
                    testApp.createPolicy(
                        orgId = organization.id,
                        bearerToken = rootUserToken,
                        policyName = "sample-policy",
                        subOrgId = subOrganizationResponse.subOrganization.name,
                        resourceName = "sample-resource",
                        actionName = "sample-action",
                        resourceInstance = "instanceId",
                    )
                assertEquals(1, samplePolicy.statements.size)
                val response =
                    testApp.client.patch(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/${subOrgUser.username}/attach_policies",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${subOrgUserCreds.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(PolicyAssociationRequest(listOf(samplePolicy.hrn))))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `user to attach policies to a different user without permission - failure`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, subOrgUserCreds) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                    )

                val samplePolicy =
                    testApp.createPolicy(
                        orgId = organization.id,
                        bearerToken = rootUserToken,
                        policyName = "sample-policy",
                        subOrgId = subOrganizationResponse.subOrganization.name,
                        resourceName = "sample-resource",
                        actionName = "sample-action",
                        resourceInstance = "instanceId",
                    )
                assertEquals(1, samplePolicy.statements.size)
                val response =
                    testApp.client.patch(
                        "/organizations/${organization
                            .id}/sub_organizations/${subOrganizationResponse.subOrganization
                            .name}/users/${subOrgUser.username}/attach_policies",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer ${subOrgUserCreds.secret}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(PolicyAssociationRequest(listOf(samplePolicy.hrn))))
                    }
                assertEquals(HttpStatusCode.Forbidden, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )
                testApp.deleteOrganization(organization.id)
            }
        }
    }

    @Nested
    @DisplayName("Create password for sub org user API tests")
    inner class CreatePasswordForSubOrganizationUserTest {
        @Test
        fun `create password for sub org user - success`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, _) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                        false,
                    )

                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = subOrgUser.username,
                    bearerToken = rootUserToken,
                    policyName = "user1-policy",
                    subOrgName = subOrganizationResponse.subOrganization.name,
                    resourceName = IamResources.USER,
                    actionName = "createPassword",
                    resourceInstance = subOrgUser.username,
                )

                // Create password for sub org user
                val response =
                    testApp.client.post(
                        "/organizations/${organization .id}" +
                            "/sub_organizations/${subOrganizationResponse.subOrganization.name}" +
                            "/users/${subOrgUser.username}/create_password",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(CreateUserPasswordRequest("testPassword@Hash1")))
                    }.apply {
                        assertEquals(HttpStatusCode.Created, status)
                        assertEquals(
                            ContentType.Application.Json,
                            contentType(),
                        )
                    }

                assertEquals(HttpStatusCode.Created, response.status)
                assertEquals(
                    ContentType.Application.Json,
                    response.contentType(),
                )

                val createPasswordResponse = gson.fromJson(response.bodyAsText(), TokenResponse::class.java)

                testApp.client.post(
                    "/organizations/${organization.id}/sub_organizations/token",
                ) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer " + createPasswordResponse.token)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        ContentType.Application.Json,
                        contentType(),
                    )
                    assertEquals(
                        organization.id,
                        headers[Constants.X_ORGANIZATION_HEADER],
                    )
                }

                // Check if password is created by logging in
                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } returns
                    AdminGetUserResponse.builder()
                        .enabled(true)
                        .username(subOrgUser.username)
                        .userAttributes(
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_EMAIL)
                                .value(subOrgUser.email).build(),
                            AttributeType.builder().name(CognitoConstants.ATTRIBUTE_PREFERRED_USERNAME)
                                .value(subOrgUser.username).build(),
                        )
                        .userCreateDate(Instant.now())
                        .build()

                val authString = "${subOrgUser.email}:testPassword@Hash1"
                val authHeader = "Basic ${Base64.getEncoder().encodeToString(authString.encodeToByteArray())}"

                val tokenResponse =
                    testApp.client.post(
                        "/organizations/${organization.id}/sub_organizations/token",
                    ) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, authHeader)
                    }
                assertEquals(HttpStatusCode.OK, tokenResponse.status)
                assertEquals(
                    ContentType.Application.Json,
                    tokenResponse.contentType(),
                )
                assertEquals(
                    organization.id,
                    tokenResponse.headers[Constants.X_ORGANIZATION_HEADER],
                )
                testApp.deleteOrganization(organization.id)
            }
        }

        @Test
        fun `create password for sub org user - failure`() {
            testSuspend {
                val (organizationResponse, _) = testApp.createOrganization()
                val organization = organizationResponse.organization
                val rootUserToken = organizationResponse.rootUserToken
                val subOrganizationResponse = testApp.createSubOrganization(organization.id, rootUserToken)
                val (subOrgUser, _) =
                    testApp.createSubOrganizationUser(
                        organization.id,
                        subOrganizationResponse.subOrganization.name,
                        rootUserToken,
                        cognitoClient,
                        false,
                    )

                testApp.createAndAttachPolicy(
                    orgId = organization.id,
                    username = subOrgUser.username,
                    bearerToken = rootUserToken,
                    policyName = "user1-policy",
                    subOrgName = subOrganizationResponse.subOrganization.name,
                    resourceName = IamResources.USER,
                    actionName = "createPassword",
                    resourceInstance = subOrgUser.username,
                )

                // Create password for sub org user
                val response =
                    testApp.client.post(
                        "/organizations/${organization .id}" +
                            "/sub_organizations/dummySubOrg" +
                            "/users/${subOrgUser.username}/create_password",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(gson.toJson(CreateUserPasswordRequest("testPassword@Hash1")))
                    }.apply {
                        assertEquals(HttpStatusCode.NotFound, status)
                        assertEquals(
                            ContentType.Application.Json,
                            contentType(),
                        )
                    }
                testApp.deleteOrganization(organization.id)
            }
        }
    }
}

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.ListCredentialResponse
import io.ktor.http.ContentType.Application.Json
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.text.Charsets.UTF_8
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException

internal class CredentialApiKtTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Nested
    @DisplayName("Create credential API tests")
    inner class CreateCredentialTest {
        @Test
        fun `without expiry - success`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)

                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                // Actual test
                val requestBody = CreateCredentialRequest()

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                    Assertions.assertEquals(
                        createdOrganization.id,
                        response.headers[Constants.X_ORGANIZATION_HEADER]
                    )

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNull(responseBody.validUntil)
                    Assertions.assertEquals(Credential.Status.active, responseBody.status)
                    Assertions.assertNotNull(responseBody.secret)
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `with expiry - success`() {
            withTestApplication(Application::handleRequest) {

                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                // Actual test
                val expiry = LocalDateTime.now().plusDays(1)
                val requestBody = CreateCredentialRequest(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Created, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(UTF_8),
                        response.contentType()
                    )

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNotNull(responseBody.validUntil)
                    Assertions.assertEquals(
                        expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), responseBody.validUntil
                    )
                    Assertions.assertEquals(Credential.Status.active, responseBody.status)
                    Assertions.assertNotNull(responseBody.secret)
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `expiry date in past - failure`() {
            withTestApplication(Application::handleRequest) {

                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                // Actual test
                val now = LocalDateTime.now().minusDays(1)
                val requestBody = CreateCredentialRequest(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                with(
                    handleRequest(
                        HttpMethod.Post,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(UTF_8), response.contentType()
                    )
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `create credential of invalid user - failure`() {
            val invalidUserName = "invalidUserName"

            // Override the cognito mock to throw error for invalid username
            coEvery {
                cognitoClient.adminGetUser(
                    match<AdminGetUserRequest>
                    { it.username() == invalidUserName }
                )
            } throws UserNotFoundException.builder().message("User does not exist.").build()

            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, _) = DataSetupHelper.createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!

                val expiry = LocalDateTime.now().plusDays(1)
                val requestBody = CreateCredentialRequest(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                with(
                    handleRequest(
                        HttpMethod.Post, "/organizations/${createdOrganization.id}/users/$invalidUserName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(
                        Json.withCharset(UTF_8), response.contentType()
                    )
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }
    }

    @Nested
    @DisplayName("Delete credential API tests")
    inner class DeleteCredentialTest {
        @Test
        fun `delete existing credential`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                val createCredentialCall = handleRequest(
                    HttpMethod.Post,
                    "/organizations/${createdOrganization.id}/users/$userName/credentials"
                ) {
                    addHeader(HttpHeaders.ContentType, Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(CreateCredentialRequest()))
                }

                val credentialsToDelete =
                    gson.fromJson(createCredentialCall.response.content, Credential::class.java)

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }

                // Validate that credential has been deleted
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                }

                // Validate that using deleted credentials returns 401
                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${credentialsToDelete.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${credentialsToDelete.secret}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status())
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `credential not found`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                val nonExistentCredentialId = UUID.randomUUID().toString()

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/$nonExistentCredentialId"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `unauthorized access`() {
            withTestApplication(Application::handleRequest) {
                val (organizationResponse1, user1) = DataSetupHelper
                    .createOrganization(this)
                val (organizationResponse2, _) = DataSetupHelper.createOrganization(this)
                val user1Name = user1.username

                val organization1 = organizationResponse1.organization!!
                val rootUserToken1 = organizationResponse1.rootUserToken!!
                val credential1 = DataSetupHelper.createCredential(organization1.id, user1Name, rootUserToken1, this)

                val rootUserToken2 = organizationResponse2.rootUserToken!!

                // Delete Credential
                with(
                    handleRequest(
                        HttpMethod.Delete,
                        "/organizations/${organization1.id}/users/$user1Name/credentials/${credential1.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken2")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.Forbidden, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
                DataSetupHelper.deleteOrganization(organization1.id, this)
                DataSetupHelper.deleteOrganization(organizationResponse2.organization!!.id, this)
            }
        }
    }

    @Nested
    @DisplayName("Get credential API tests")
    inner class GetCredentialTest {
        @Test
        fun `success - response does not have secret`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                val createdCredentials = DataSetupHelper
                    .createCredential(createdOrganization.id, userName, rootUserToken, this)

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${createdCredentials.id}"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())

                    val responseBody = gson.fromJson(response.content, Credential::class.java)
                    Assertions.assertNull(responseBody.validUntil)
                    Assertions.assertNull(responseBody.secret)
                    Assertions.assertEquals(Credential.Status.active, responseBody.status)
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `not found`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username
                val nonExistentCredentialId = UUID.randomUUID().toString()

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/$nonExistentCredentialId"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }

                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }

        @Test
        fun `invalid credential id`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/inValid_credential_id"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.BadRequest, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
                DataSetupHelper.deleteOrganization(createdOrganization.id, this)
            }
        }
    }

    @Nested
    @DisplayName("List credentials API tests")
    inner class ListCredentialTest {
        @Test
        fun `list credentials of a user`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                val credential1 =
                    DataSetupHelper.createCredential(createdOrganization.id, userName, rootUserToken, this)
                val credential2 =
                    DataSetupHelper.createCredential(createdOrganization.id, userName, rootUserToken, this)

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())

                    val responseBody = gson.fromJson(response.content, ListCredentialResponse::class.java)
                    Assertions.assertEquals(2, responseBody.credentials.size)
                    Assertions.assertEquals(credential1.id, responseBody.credentials[0].id)
                    Assertions.assertEquals(credential2.id, responseBody.credentials[1].id)
                }
            }
        }

        @Test
        fun `list credentials for unknown user`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, _) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } throws UserNotFoundException.builder().message("User does not exist.").build()

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/unknown_user/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.NotFound, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())
                }
            }
        }

        @Test
        fun `return empty list for credentials list api`() {
            withTestApplication(Application::handleRequest) {
                val (createdOrganizationResponse, createdUser) = DataSetupHelper
                    .createOrganization(this)
                val createdOrganization = createdOrganizationResponse.organization!!
                val rootUserToken = createdOrganizationResponse.rootUserToken!!
                val userName = createdUser.username

                with(
                    handleRequest(
                        HttpMethod.Get,
                        "/organizations/${createdOrganization.id}/users/$userName/credentials"
                    ) {
                        addHeader(HttpHeaders.ContentType, Json.toString())
                        addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                ) {
                    Assertions.assertEquals(HttpStatusCode.OK, response.status())
                    Assertions.assertEquals(Json.withCharset(UTF_8), response.contentType())

                    val responseBody = gson.fromJson(response.content, ListCredentialResponse::class.java)
                    Assertions.assertEquals(0, responseBody.credentials.size)
                }
            }
        }
    }
}

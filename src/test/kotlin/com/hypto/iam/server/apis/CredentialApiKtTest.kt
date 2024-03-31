package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createCredential
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV2.deleteOrganization
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.ListCredentialResponse
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.koin.test.inject
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class CredentialApiKtTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Nested
    @DisplayName("Create credential API tests")
    inner class CreateCredentialTest {
        @Test
        fun `without expiry - success`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()

                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                // Actual test
                val requestBody = CreateCredentialRequest()
                val response =
                    client.post(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.Created, response.status)
                Assertions.assertEquals(Json, response.contentType())
                Assertions.assertEquals(
                    createdOrganization.id,
                    response.headers[Constants.X_ORGANIZATION_HEADER],
                )

                val responseBody = gson.fromJson(response.bodyAsText(), Credential::class.java)
                Assertions.assertNull(responseBody.validUntil)
                Assertions.assertEquals(Credential.Status.active, responseBody.status)
                Assertions.assertNotNull(responseBody.secret)

                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `with expiry - success`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                // Actual test
                val expiry = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS)
                val requestBody = CreateCredentialRequest(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                val response =
                    client.post(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.Created, response.status)
                Assertions.assertEquals(
                    Json,
                    response.contentType(),
                )

                val responseBody = gson.fromJson(response.bodyAsText(), Credential::class.java)
                Assertions.assertNotNull(responseBody.validUntil)
                Assertions.assertEquals(
                    expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    responseBody.validUntil,
                )
                Assertions.assertEquals(Credential.Status.active, responseBody.status)
                Assertions.assertNotNull(responseBody.secret)

                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `expiry date in past - failure`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                // Actual test
                val now = LocalDateTime.now().minusDays(1)
                val requestBody = CreateCredentialRequest(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                val response =
                    client.post(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
                Assertions.assertEquals(
                    Json,
                    response.contentType(),
                )
                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `create credential of invalid user - failure`() {
            val invalidUserName = "invalidUserName"

            // Override the cognito mock to throw error for invalid username
            coEvery {
                cognitoClient.adminGetUser(
                    match<AdminGetUserRequest> {
                        it.username() == invalidUserName
                    },
                )
            } throws UserNotFoundException.builder().message("User does not exist.").build()

            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, _) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken

                val expiry = LocalDateTime.now().plusDays(1)
                val requestBody = CreateCredentialRequest(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                val response =
                    client.post(
                        "/organizations/${createdOrganization.id}/users/$invalidUserName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(requestBody))
                    }
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(
                    Json,
                    response.contentType(),
                )
                deleteOrganization(createdOrganization.id)
            }
        }
    }

    @Nested
    @DisplayName("Delete credential API tests")
    inner class DeleteCredentialTest {
        @Test
        fun `delete existing credential`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                val createCredentialCall =
                    client.post(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                        setBody(gson.toJson(CreateCredentialRequest()))
                    }

                val credentialsToDelete =
                    gson.fromJson(createCredentialCall.bodyAsText(), Credential::class.java)

                // Delete Credential
                var response =
                    client.delete(
                        "/organizations/${createdOrganization.id}/users/" +
                            "$userName/credentials/${credentialsToDelete.id}",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(Json, response.contentType())

                // Validate that credential has been deleted
                response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/" +
                            "$userName/credentials/${credentialsToDelete.id}",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)

                // Validate that using deleted credentials returns 401
                response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/" +
                            "$userName/credentials/${credentialsToDelete.id}",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer ${credentialsToDelete.secret}")
                    }
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)

                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `credential not found`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                val nonExistentCredentialId = UUID.randomUUID().toString()

                // Delete Credential
                val response =
                    client.delete(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/$nonExistentCredentialId",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(Json, response.contentType())

                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `unauthorized access`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (organizationResponse1, user1) = createOrganization()
                val (organizationResponse2, _) = createOrganization()
                val user1Name = organizationResponse1.organization.rootUser.username

                val organization1 = organizationResponse1.organization
                val rootUserToken1 = organizationResponse1.rootUserToken
                val credential1 = createCredential(organization1.id, user1Name, rootUserToken1)

                val rootUserToken2 = organizationResponse2.rootUserToken

                // Delete Credential
                val response =
                    client.delete(
                        "/organizations/${organization1.id}/users/$user1Name/credentials/${credential1.id}",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken2")
                    }
                Assertions.assertEquals(HttpStatusCode.Forbidden, response.status)
                Assertions.assertEquals(Json, response.contentType())
                deleteOrganization(organization1.id)
                deleteOrganization(organizationResponse2.organization.id)
            }
        }
    }

    @Nested
    @DisplayName("Get credential API tests")
    inner class GetCredentialTest {
        @Test
        fun `success - response does not have secret`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                val createdCredentials = createCredential(createdOrganization.id, userName, rootUserToken)

                val response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/${createdCredentials.id}",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    }
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(Json, response.contentType())

                val responseBody = gson.fromJson(response.bodyAsText(), Credential::class.java)
                Assertions.assertNull(responseBody.validUntil)
                Assertions.assertNull(responseBody.secret)
                Assertions.assertEquals(Credential.Status.active, responseBody.status)

                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `not found`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username
                val nonExistentCredentialId = UUID.randomUUID().toString()

                val response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/$nonExistentCredentialId",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(Json, response.contentType())

                deleteOrganization(createdOrganization.id)
            }
        }

        @Test
        fun `invalid credential id`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                val response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials/inValid_credential_id",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
                Assertions.assertEquals(Json, response.contentType())
                deleteOrganization(createdOrganization.id)
            }
        }
    }

    @Nested
    @DisplayName("List credentials API tests")
    inner class ListCredentialTest {
        @Test
        fun `list credentials of a user`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                val credential1 =
                    createCredential(createdOrganization.id, userName, rootUserToken)
                val credential2 =
                    createCredential(createdOrganization.id, userName, rootUserToken)

                val response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(Json, response.contentType())

                val responseBody = gson.fromJson(response.bodyAsText(), ListCredentialResponse::class.java)
                Assertions.assertEquals(2, responseBody.credentials.size)
                Assertions.assertEquals(credential1.id, responseBody.credentials[0].id)
                Assertions.assertEquals(credential2.id, responseBody.credentials[1].id)
            }
        }

        @Test
        fun `list credentials for unknown user`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, _) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken

                coEvery {
                    cognitoClient.adminGetUser(any<AdminGetUserRequest>())
                } throws UserNotFoundException.builder().message("User does not exist.").build()

                val response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/unknown_user/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
                Assertions.assertEquals(Json, response.contentType())
            }
        }

        @Test
        fun `return empty list for credentials list api`() {
            testApplication {
                environment {
                    config = ApplicationConfig("application-custom.conf")
                }
                val (createdOrganizationResponse, createdUser) = createOrganization()
                val createdOrganization = createdOrganizationResponse.organization
                val rootUserToken = createdOrganizationResponse.rootUserToken
                val userName = createdOrganizationResponse.organization.rootUser.username

                val response =
                    client.get(
                        "/organizations/${createdOrganization.id}/users/$userName/credentials",
                    ) {
                        header(HttpHeaders.ContentType, Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    }
                Assertions.assertEquals(HttpStatusCode.OK, response.status)
                Assertions.assertEquals(Json, response.contentType())

                val responseBody = gson.fromJson(response.bodyAsText(), ListCredentialResponse::class.java)
                Assertions.assertEquals(0, responseBody.credentials.size)
            }
        }
    }
}

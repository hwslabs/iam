package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV2.deleteOrganization
import com.hypto.iam.server.models.CreateSubOrganizationRequest
import com.hypto.iam.server.models.CreateSubOrganizationResponse
import com.hypto.iam.server.models.SubOrganization
import com.hypto.iam.server.models.SubOrganizationsPaginatedResponse
import com.hypto.iam.server.models.UpdateSubOrganizationRequest
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
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.koin.test.inject
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@Testcontainers
internal class SubOrganizationApiKtTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `create a sub organization under an organization`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgName = "test-sub-org" + IdGenerator.randomId()
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id
            val token = organizationResponse.rootUserToken

            val requestBody =
                CreateSubOrganizationRequest(
                    subOrgName,
                )
            val response =
                client.post("/organizations/$orgId/sub_organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateSubOrganizationResponse::class.java)

            // Assert API response
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            assertEquals(subOrgName, responseBody.subOrganization.name, "Sub organization name should match")
            assertEquals(
                orgId,
                responseBody.subOrganization.organizationId,
                "Sub organization parent id should " +
                    "match",
            )

            deleteOrganization(orgId)
        }
    }

    @Test
    fun `create sub organization with invalid credentials`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgName = "test-sub-org" + IdGenerator.randomId()
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id

            val requestBody =
                CreateSubOrganizationRequest(
                    subOrgName,
                )
            val response =
                client.post("/organizations/$orgId/sub_organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer test-bearer-token")
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateSubOrganizationResponse::class.java)
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertFalse(response.headers.contains(HttpHeaders.ContentType))
            assertNull(response.headers[Constants.X_ORGANIZATION_HEADER])
            deleteOrganization(orgId)
        }
    }

    @Test
    fun `get sub organization success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgName = "test-sub-org" + IdGenerator.randomId()
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id
            val token = organizationResponse.rootUserToken
            val subOrgDescription = "test-sub-org-desc"

            val requestBody =
                CreateSubOrganizationRequest(
                    subOrgName,
                    subOrgDescription,
                )
            val response =
                client.post("/organizations/$orgId/sub_organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateSubOrganizationResponse::class.java)

            // Assert API response
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            assertEquals(subOrgName, responseBody.subOrganization.name, "Sub organization name should match")
            assertEquals(
                orgId,
                responseBody.subOrganization.organizationId,
                "Sub organization parent id should " +
                    "match",
            )

            // Get sub organization
            val getSubOrganizationResponse =
                client.get("/organizations/$orgId/sub_organizations/$subOrgName") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                }
            assertEquals(HttpStatusCode.OK, getSubOrganizationResponse.status)
            val subOrganization =
                gson.fromJson(getSubOrganizationResponse.bodyAsText(), SubOrganization::class.java)
            assertEquals(subOrgName, subOrganization.name, "Sub organization name should match")
            assertEquals(subOrgDescription, subOrganization.description, "Sub organization description should match")
            deleteOrganization(orgId)
        }
    }

    @Test
    fun `get organization not found`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgName = "test-sub-org" + IdGenerator.randomId()
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id
            val token = organizationResponse.rootUserToken
            val subOrgDescription = "test-sub-org-desc"

            // Get sub organization
            val getSubOrganizationResponse =
                client.get("/organizations/$orgId/sub_organizations/dummy_sub_org") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                }
            assertEquals(HttpStatusCode.NotFound, getSubOrganizationResponse.status)
            deleteOrganization(orgId)
        }
    }

    @Test
    fun `update sub organization desc success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgName = "test-sub-org" + IdGenerator.randomId()
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id
            val token = organizationResponse.rootUserToken
            val subOrgDescription = "test-sub-org-desc"

            val requestBody =
                CreateSubOrganizationRequest(
                    subOrgName,
                    subOrgDescription,
                )
            val response =
                client.post("/organizations/$orgId/sub_organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateSubOrganizationResponse::class.java)

            // Assert API response
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            assertEquals(subOrgName, responseBody.subOrganization.name, "Sub organization name should match")
            assertEquals(
                orgId,
                responseBody.subOrganization.organizationId,
                "Sub organization parent id should " +
                    "match",
            )

            // Update sub organization name
            val updateSubOrganizationNameRequest =
                UpdateSubOrganizationRequest(
                    "updated-sub-org-desc",
                )
            val updateSubOrganizationNameResponse =
                client
                    .patch("/organizations/$orgId/sub_organizations/$subOrgName") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header("Authorization", "Bearer $token")
                        setBody(gson.toJson(updateSubOrganizationNameRequest))
                    }
            assertEquals(HttpStatusCode.OK, updateSubOrganizationNameResponse.status)
            val updatedSubOrganization =
                gson.fromJson(updateSubOrganizationNameResponse.bodyAsText(), SubOrganization::class.java)
            assertEquals(
                "updated-sub-org-desc",
                updatedSubOrganization.description,
                "Sub organization description should match",
            )
            deleteOrganization(orgId)
        }
    }

    @Test
    fun `delete sub organization success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgName = "test-sub-org" + IdGenerator.randomId()
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id
            val token = organizationResponse.rootUserToken
            val subOrgDescription = "test-sub-org-desc"

            val requestBody =
                CreateSubOrganizationRequest(
                    subOrgName,
                    subOrgDescription,
                )
            val response =
                client.post("/organizations/$orgId/sub_organizations") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                    setBody(gson.toJson(requestBody))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), CreateSubOrganizationResponse::class.java)

            // Assert API response
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            assertEquals(subOrgName, responseBody.subOrganization.name, "Sub organization name should match")
            assertEquals(
                orgId,
                responseBody.subOrganization.organizationId,
                "Sub organization parent id should " +
                    "match",
            )

            // Delete sub organization
            val deleteSubOrganizationResponse =
                client.delete("/organizations/$orgId/sub_organizations/$subOrgName") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                }
            assertEquals(HttpStatusCode.OK, deleteSubOrganizationResponse.status)

            // Get sub organization
            val getSubOrganizationResponse =
                client.get("/organizations/$orgId/sub_organizations/$subOrgName") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("Authorization", "Bearer $token")
                }
            assertEquals(HttpStatusCode.NotFound, getSubOrganizationResponse.status)

            deleteOrganization(orgId)
        }
    }

    @Test
    fun `list sub organization success`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val subOrgCount = 50
            val (organizationResponse, _) = createOrganization()
            val orgId = organizationResponse.organization.id
            val token = organizationResponse.rootUserToken
            val originalSubOrgList = mutableListOf<SubOrganization>()
            repeat(subOrgCount) {
                val subOrgName = "test-sub-org" + IdGenerator.randomId() + "$it"
                val subOrgDescription = "test-sub-org-desc$it"
                val requestBody =
                    CreateSubOrganizationRequest(
                        subOrgName,
                        subOrgDescription,
                    )
                val response =
                    client.post("/organizations/$orgId/sub_organizations") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header("Authorization", "Bearer $token")
                        setBody(gson.toJson(requestBody))
                    }
                val responseBody = gson.fromJson(response.bodyAsText(), CreateSubOrganizationResponse::class.java)

                // Assert API response
                assertEquals(HttpStatusCode.Created, response.status)
                assertEquals(ContentType.Application.Json, response.contentType())

                assertEquals(subOrgName, responseBody.subOrganization.name, "Sub organization name should match")
                assertEquals(
                    orgId,
                    responseBody.subOrganization.organizationId,
                    "Sub organization parent id should " +
                        "match",
                )
                originalSubOrgList.add(responseBody.subOrganization)
            }

            // List sub organization
            val subOrganizationList = mutableListOf<SubOrganization>()
            var nextToken: String? = null
            do {
                val url =
                    if (nextToken == null) {
                        "/organizations/$orgId/sub_organizations"
                    } else {
                        "/organizations/$orgId/sub_organizations?next_token=$nextToken"
                    }

                val listSubOrganizationResponse =
                    client.get(url) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header("Authorization", "Bearer $token")
                    }
                assertEquals(HttpStatusCode.OK, listSubOrganizationResponse.status)
                val subOrganizationPaginatedResponse =
                    gson.fromJson(
                        listSubOrganizationResponse.bodyAsText(),
                        SubOrganizationsPaginatedResponse::class.java,
                    )
                subOrganizationPaginatedResponse.data?.let { subOrganizationList.addAll(it) }
                nextToken = subOrganizationPaginatedResponse.nextToken
            } while (subOrganizationPaginatedResponse.nextToken != null)
            assertEquals(50, subOrganizationList.size, "Sub organization list size should match")
            assertEquals(originalSubOrgList.toSet(), subOrganizationList.toSet(), "Sub organization list should match")

            deleteOrganization(orgId)
        }
    }
}

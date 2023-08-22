package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV2.createResource
import com.hypto.iam.server.helpers.DataSetupHelperV2.deleteOrganization
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.Resource
import com.hypto.iam.server.models.ResourcePaginatedResponse
import com.hypto.iam.server.models.UpdateResourceRequest
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
import io.ktor.http.withCharset
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.text.Charsets.UTF_8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.inject
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
internal class ResourceApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `create resource success case`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resourceName = "resource-name"

            val response = client.post("/organizations/${organization.id}/resources") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(CreateResourceRequest(name = resourceName)))
            }
            val responseBody = gson.fromJson(response.bodyAsText(), Resource::class.java)
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

            assertEquals(resourceName, responseBody.name)
            assertEquals(organization.id, responseBody.organizationId)
            assertEquals("", responseBody.description)

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `get resource`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resourceName = "resource-name"
            createResource(organization.id, rootUserToken, resourceName)

            val response = client.get("/organizations/${organization.id}/resources/$resourceName") {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

            val responseBody = gson.fromJson(response.bodyAsText(), Resource::class.java)
            assertEquals(resourceName, responseBody.name)

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `get resource failure for unauthorized user access`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse1, _) = createOrganization()
            val organization1 = organizationResponse1.organization
            val rootUserToken1 = organizationResponse1.rootUserToken

            val (organizationResponse2, _) = createOrganization()
            val organization2 = organizationResponse2.organization
            val rootUserToken2 = organizationResponse2.rootUserToken

            val resource = createResource(organization1.id, rootUserToken1)

            val response = client.get("/organizations/${organization1.id}/resources/${resource.name}") {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken2")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

            deleteOrganization(organization1.id)
            deleteOrganization(organization2.id)
        }
    }

    @Test
    fun `delete resource`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resource = createResource(organization.id, rootUserToken)

            val response = client.delete("/organizations/${organization.id}/resources/${resource.name}") {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())
            val responseBody = gson.fromJson(response.bodyAsText(), BaseSuccessResponse::class.java)
            assertEquals(true, responseBody.success)

            // Check that the resource is no longer available
            val getResourceResponse = client.get("/organizations/${organization.id}/resources/${resource.name}") {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.NotFound, getResourceResponse.status)

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `list resources within a organization`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resource1 = createResource(organization.id, rootUserToken)
            val resource2 = createResource(organization.id, rootUserToken)

            val response = client.get("/organizations/${organization.id}/resources") {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

            val responseBody = gson.fromJson(response.bodyAsText(), ResourcePaginatedResponse::class.java)
            assertEquals(responseBody.data!!.size, 2)
            assert(responseBody.data!!.contains(resource1))
            assert(responseBody.data!!.contains(resource2))

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `update resource`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resource = createResource(organization.id, rootUserToken)
            val newDescription = "new description"

            val response = client.patch("/organizations/${organization.id}/resources/${resource.name}") {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(UpdateResourceRequest(description = "new description")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

            val responseBody = gson.fromJson(response.bodyAsText(), Resource::class.java)
            assertEquals(resource.name, responseBody.name)
            assertEquals(newDescription, responseBody.description)

            deleteOrganization(organization.id)
        }
    }
}

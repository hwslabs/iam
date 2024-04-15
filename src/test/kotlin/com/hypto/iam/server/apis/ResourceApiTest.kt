package com.hypto.iam.server.apis

import com.hypto.iam.server.helpers.BaseSingleAppTest
import com.hypto.iam.server.helpers.DataSetupHelperV3.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV3.createResource
import com.hypto.iam.server.helpers.DataSetupHelperV3.deleteOrganization
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
import io.ktor.test.dispatcher.testSuspend
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
internal class ResourceApiTest : BaseSingleAppTest() {
    @Test
    fun `create resource success case`() {
        testSuspend {
            val (organizationResponse, _) = testApp.createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resourceName = "resource-name"

            val response =
                testApp.client.post("/organizations/${organization.id}/resources") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(CreateResourceRequest(name = resourceName)))
                }
            val responseBody = gson.fromJson(response.bodyAsText(), Resource::class.java)
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            assertEquals(resourceName, responseBody.name)
            assertEquals(organization.id, responseBody.organizationId)
            assertEquals("", responseBody.description)

            testApp.deleteOrganization(organization.id)
        }
    }

    @Test
    fun `get resource`() {
        testSuspend {
            val (organizationResponse, _) = testApp.createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resourceName = "resource-name"
            testApp.createResource(organization.id, rootUserToken, resourceName)

            val response =
                testApp.client.get("/organizations/${organization.id}/resources/$resourceName") {
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            val responseBody = gson.fromJson(response.bodyAsText(), Resource::class.java)
            assertEquals(resourceName, responseBody.name)

            testApp.deleteOrganization(organization.id)
        }
    }

    @Test
    fun `get resource failure for unauthorized user access`() {
        testSuspend {
            val (organizationResponse1, _) = testApp.createOrganization()
            val organization1 = organizationResponse1.organization
            val rootUserToken1 = organizationResponse1.rootUserToken

            val (organizationResponse2, _) = testApp.createOrganization()
            val organization2 = organizationResponse2.organization
            val rootUserToken2 = organizationResponse2.rootUserToken

            val resource = testApp.createResource(organization1.id, rootUserToken1)

            val response =
                testApp.client.get("/organizations/${organization1.id}/resources/${resource.name}") {
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken2")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            testApp.deleteOrganization(organization1.id)
            testApp.deleteOrganization(organization2.id)
        }
    }

    @Test
    fun `delete resource`() {
        testSuspend {
            val (organizationResponse, _) = testApp.createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resource = testApp.createResource(organization.id, rootUserToken)

            val response =
                testApp.client.delete("/organizations/${organization.id}/resources/${resource.name}") {
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())
            val responseBody = gson.fromJson(response.bodyAsText(), BaseSuccessResponse::class.java)
            assertEquals(true, responseBody.success)

            // Check that the resource is no longer available
            val getResourceResponse =
                testApp.client.get("/organizations/${organization.id}/resources/${resource.name}") {
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            assertEquals(HttpStatusCode.NotFound, getResourceResponse.status)

            testApp.deleteOrganization(organization.id)
        }
    }

    @Test
    fun `list resources within a organization`() {
        testSuspend {
            val (organizationResponse, _) = testApp.createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resource1 = testApp.createResource(organization.id, rootUserToken)
            val resource2 = testApp.createResource(organization.id, rootUserToken)

            val response =
                testApp.client.get("/organizations/${organization.id}/resources") {
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            val responseBody = gson.fromJson(response.bodyAsText(), ResourcePaginatedResponse::class.java)
            assertEquals(responseBody.data!!.size, 2)
            assert(responseBody.data!!.contains(resource1))
            assert(responseBody.data!!.contains(resource2))

            testApp.deleteOrganization(organization.id)
        }
    }

    @Test
    fun `update resource`() {
        testSuspend {
            val (organizationResponse, _) = testApp.createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val resource = testApp.createResource(organization.id, rootUserToken)
            val newDescription = "new description"

            val response =
                testApp.client.patch("/organizations/${organization.id}/resources/${resource.name}") {
                    header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(UpdateResourceRequest(description = "new description")))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType())

            val responseBody = gson.fromJson(response.bodyAsText(), Resource::class.java)
            assertEquals(resource.name, responseBody.name)
            assertEquals(newDescription, responseBody.description)

            testApp.deleteOrganization(organization.id)
        }
    }
}

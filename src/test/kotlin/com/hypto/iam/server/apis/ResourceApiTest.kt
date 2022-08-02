package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.Resource
import com.hypto.iam.server.models.ResourcePaginatedResponse
import com.hypto.iam.server.models.UpdateResourceRequest
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
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            val resourceName = "resource-name"

            with(
                handleRequest(HttpMethod.Post, "/organizations/${organization.id}/resources") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    setBody(gson.toJson(CreateResourceRequest(name = resourceName)))
                }
            ) {
                val responseBody = gson.fromJson(response.content, Resource::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                assertEquals(resourceName, responseBody.name)
                assertEquals(organization.id, responseBody.organizationId)
                assertEquals("", responseBody.description)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get resource`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            val resourceName = "resource-name"
            DataSetupHelper.createResource(organization.id, rootUserToken, this, resourceName)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/resources/$resourceName") {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val responseBody = gson.fromJson(response.content, Resource::class.java)
                assertEquals(resourceName, responseBody.name)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get resource failure for unauthorized user access`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse1, _) = DataSetupHelper.createOrganization(this)
            val organization1 = organizationResponse1.organization!!
            val rootUserToken1 = organizationResponse1.rootUserToken!!

            val (organizationResponse2, _) = DataSetupHelper.createOrganization(this)
            val organization2 = organizationResponse2.organization!!
            val rootUserToken2 = organizationResponse2.rootUserToken!!

            val resource = DataSetupHelper.createResource(organization1.id, rootUserToken1, this)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization1.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken2")
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())
            }

            DataSetupHelper.deleteOrganization(organization1.id, this)
            DataSetupHelper.deleteOrganization(organization2.id, this)
        }
    }

    @Test
    fun `delete resource`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            val resource = DataSetupHelper.createResource(organization.id, rootUserToken, this)

            with(
                handleRequest(HttpMethod.Delete, "/organizations/${organization.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())
                val responseBody = gson.fromJson(response.content, BaseSuccessResponse::class.java)
                assertEquals(true, responseBody.success)
            }

            // Check that the resource is no longer available
            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            ) {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `list resources within a organization`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            val resource1 = DataSetupHelper.createResource(organization.id, rootUserToken, this)
            val resource2 = DataSetupHelper.createResource(organization.id, rootUserToken, this)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/resources") {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val responseBody = gson.fromJson(response.content, ResourcePaginatedResponse::class.java)
                assertEquals(responseBody.data!!.size, 2)
                assert(responseBody.data!!.contains(resource1))
                assert(responseBody.data!!.contains(resource2))
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `update resource`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val rootUserToken = organizationResponse.rootUserToken!!

            val resource = DataSetupHelper.createResource(organization.id, rootUserToken, this)
            val newDescription = "new description"

            with(
                handleRequest(HttpMethod.Patch, "/organizations/${organization.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer $rootUserToken")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(UpdateResourceRequest(description = "new description")))
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val responseBody = gson.fromJson(response.content, Resource::class.java)
                assertEquals(resource.name, responseBody.name)
                assertEquals(newDescription, responseBody.description)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }
}

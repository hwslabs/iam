package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.helpers.MockStore
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.Resource
import com.hypto.iam.server.models.ResourcePaginatedResponse
import com.hypto.iam.server.models.UpdateResourceRequest
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
import io.mockk.mockkClass
import kotlin.text.Charsets.UTF_8
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
internal class ResourceApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(repositoryModule, controllerModule, applicationModule)
    }

    @JvmField
    @RegisterExtension
    val koinMockProvider = MockProviderExtension.create { mockkClass(it) }

    private val mockStore = MockStore()

    @AfterEach
    fun tearDown() {
        mockStore.clear()
    }

    @Test
    fun `create resource`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val resourceName = "resource-name"

            with(
                handleRequest(HttpMethod.Post, "/organizations/${organization.id}/resources") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    setBody(gson.toJson(CreateResourceRequest(name = resourceName)))
                }
            ) {
                val responseBody = gson.fromJson(response.content, Resource::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                assertEquals(resourceName, responseBody.name)
                assertEquals(organization.id, responseBody.organizationId)
                assertEquals(responseBody.description, "")
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get resource`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val resourceName = "resource-name"
            DataSetupHelper.createResource(organization.id, createdCredentials, this, resourceName)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/resources/$resourceName") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val responseBody = gson.fromJson(response.content, Resource::class.java)
                assertEquals(responseBody.name, resourceName)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get resource from different organization user`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse1, _) = DataSetupHelper.createOrganization(this)
            val organization1 = organizationResponse1.organization!!
            val createdCredentials1 = organizationResponse1.adminUserCredential!!

            val (organizationResponse2, _) = DataSetupHelper.createOrganization(this)
            val organization2 = organizationResponse2.organization!!
            val createdCredentials2 = organizationResponse2.adminUserCredential!!

            val resource = DataSetupHelper.createResource(organization1.id, createdCredentials1, this)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization1.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials2.secret}")
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
            val createdCredentials = organizationResponse.adminUserCredential!!

            val resource = DataSetupHelper.createResource(organization.id, createdCredentials, this)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `list resources within a organization`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val resource1 = DataSetupHelper.createResource(organization.id, createdCredentials, this)
            val resource2 = DataSetupHelper.createResource(organization.id, createdCredentials, this)

            with(
                handleRequest(HttpMethod.Get, "/organizations/${organization.id}/resources") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
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
            val createdCredentials = organizationResponse.adminUserCredential!!

            val resource = DataSetupHelper.createResource(organization.id, createdCredentials, this)
            val newDescription = "new description"

            with(
                handleRequest(HttpMethod.Patch, "/organizations/${organization.id}/resources/${resource.name}") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(UpdateResourceRequest(description = "new description")))
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(UTF_8), response.contentType())

                val responseBody = gson.fromJson(response.content, Resource::class.java)
                assertEquals(responseBody.name, resource.name)
                assertEquals(responseBody.description, newDescription)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }
}

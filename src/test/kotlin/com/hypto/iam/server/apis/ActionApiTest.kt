package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.handleRequest
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.ActionPaginatedResponse
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.UpdateActionRequest
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActionApiTest : AbstractContainerBaseTest() {
    private val gson = Gson()

    @Test
    fun `create action success case`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!
            val resource = DataSetupHelper.createResource(organization.id, createdCredentials, this)

            with(
                handleRequest(HttpMethod.Post, "/organizations/${organization.id}/resources/${resource.name}/actions") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    setBody(gson.toJson(CreateActionRequest(name = "test-action")))
                }
            ) {
                val responseBody = gson.fromJson(response.content, Action::class.java)
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                assertEquals(organization.id, responseBody.organizationId)
                assertEquals("", responseBody.description)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get action`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val (action, resource) = DataSetupHelper.createAction(organization.id, null, createdCredentials, this)

            with(
                handleRequest(
                    HttpMethod.Get,
                    "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                val responseBody = gson.fromJson(response.content, Action::class.java)
                assertEquals(action.name, responseBody.name)
                assertEquals(organization.id, responseBody.organizationId)
                assertEquals(resource.name, responseBody.resourceName)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `get action failure for unauthorized user access`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse1, _) = DataSetupHelper.createOrganization(this)
            val organization1 = organizationResponse1.organization!!
            val createdCredentials1 = organizationResponse1.adminUserCredential!!

            val (organizationResponse2, _) = DataSetupHelper.createOrganization(this)
            val organization2 = organizationResponse2.organization!!
            val createdCredentials2 = organizationResponse2.adminUserCredential!!

            val (action, resource) = DataSetupHelper.createAction(organization1.id, null, createdCredentials1, this)

            with(
                handleRequest(
                    HttpMethod.Get,
                    "/organizations/${organization1.id}/resources/${resource.name}/actions/${action.name}"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials2.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )
            }

            DataSetupHelper.deleteOrganization(organization1.id, this)
            DataSetupHelper.deleteOrganization(organization2.id, this)
        }
    }

    @Test
    fun `delete action`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val (action, resource) = DataSetupHelper.createAction(organization.id, null, createdCredentials, this)

            with(
                handleRequest(
                    HttpMethod.Delete,
                    "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
                val responseBody = gson.fromJson(response.content, BaseSuccessResponse::class.java)
                assertEquals(true, responseBody.success)
            }

            // Check that the action is not available
            with(
                handleRequest(
                    HttpMethod.Get,
                    "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `list actions for a resource`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val (action1, resource) = DataSetupHelper.createAction(organization.id, null, createdCredentials, this)
            val (action2, _) = DataSetupHelper.createAction(organization.id, resource, createdCredentials, this)

            with(
                handleRequest(
                    HttpMethod.Get,
                    "/organizations/${organization.id}/resources/${resource.name}/actions"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                val responseBody = gson.fromJson(response.content, ActionPaginatedResponse::class.java)
                assertEquals(2, responseBody.data!!.size)
                assert(responseBody.data!!.contains(action1))
                assert(responseBody.data!!.contains(action2))
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }

    @Test
    fun `update action`() {
        withTestApplication(Application::handleRequest) {
            val (organizationResponse, _) = DataSetupHelper.createOrganization(this)
            val organization = organizationResponse.organization!!
            val createdCredentials = organizationResponse.adminUserCredential!!

            val (action, resource) = DataSetupHelper.createAction(organization.id, null, createdCredentials, this)
            val newDescription = "new description"

            with(
                handleRequest(
                    HttpMethod.Patch,
                    "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
                ) {
                    addHeader(HttpHeaders.Authorization, "Bearer ${createdCredentials.secret}")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(gson.toJson(UpdateActionRequest(description = "new description")))
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    response.contentType()
                )

                val responseBody = gson.fromJson(response.content, Action::class.java)
                assertEquals(action.name, responseBody.name)
                assertEquals(resource.name, responseBody.resourceName)
                assertEquals(newDescription, responseBody.description)
            }

            DataSetupHelper.deleteOrganization(organization.id, this)
        }
    }
}

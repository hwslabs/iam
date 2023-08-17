package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.Constants
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelperV2.createAction
import com.hypto.iam.server.helpers.DataSetupHelperV2.createOrganization
import com.hypto.iam.server.helpers.DataSetupHelperV2.createResource
import com.hypto.iam.server.helpers.DataSetupHelperV2.deleteOrganization
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.ActionPaginatedResponse
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.UpdateActionRequest
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.inject

class ActionApiTest : AbstractContainerBaseTest() {
    private val gson: Gson by inject()

    @Test
    fun `create action success case1`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()

            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken
            val resource = createResource(organization.id, rootUserToken)
            val response = client.post("/organizations/${organization.id}/resources/${resource.name}/actions") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                setBody(gson.toJson(CreateActionRequest(name = "test-action")))
            }
            val responseBody = gson.fromJson(response.bodyAsText(), Action::class.java)
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )
            assertEquals(
                organization.id,
                response.headers[Constants.X_ORGANIZATION_HEADER]
            )

            assertEquals(organization.id, responseBody.organizationId)
            assertEquals("", responseBody.description)

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `get action`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val (action, resource) = createAction(organization.id, null, rootUserToken)

            val response = client.get(
                "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
            ) {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )

            val responseBody = gson.fromJson(response.bodyAsText(), Action::class.java)
            assertEquals(action.name, responseBody.name)
            assertEquals(organization.id, responseBody.organizationId)
            assertEquals(resource.name, responseBody.resourceName)

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `get action failure for unauthorized user access`() {
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

            val (action, resource) = createAction(organization1.id, null, rootUserToken1)

            val response = client.get(
                "/organizations/${organization1.id}/resources/${resource.name}/actions/${action.name}"
            ) {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken2")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )

            deleteOrganization(organization1.id)
            deleteOrganization(organization2.id)
        }
    }

    @Test
    fun `delete action`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val (action, resource) = createAction(organization.id, null, rootUserToken)
            var response = client.delete(
                "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
            ) {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            val responseBody = gson.fromJson(response.bodyAsText(), BaseSuccessResponse::class.java)
            assertEquals(true, responseBody.success)

            // Check that the action is not available
            response = client.get(
                "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
            ) {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.NotFound, response.status)

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `list actions for a resource`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val (action1, resource) = createAction(organization.id, null, rootUserToken,)
            val (action2, _) = createAction(organization.id, resource, rootUserToken)
            val response = client.get(
                "/organizations/${organization.id}/resources/${resource.name}/actions"
            ) {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )

            val responseBody = gson.fromJson(response.bodyAsText(), ActionPaginatedResponse::class.java)
            assertEquals(2, responseBody.data!!.size)
            assert(responseBody.data!!.contains(action1))
            assert(responseBody.data!!.contains(action2))

            deleteOrganization(organization.id)
        }
    }

    @Test
    fun `update action`() {
        testApplication {
            environment {
                config = ApplicationConfig("application-custom.conf")
            }
            val (organizationResponse, _) = createOrganization()
            val organization = organizationResponse.organization
            val rootUserToken = organizationResponse.rootUserToken

            val (action, resource) = createAction(organization.id, null, rootUserToken)
            val newDescription = "new description"
            val response = client.patch(
                "/organizations/${organization.id}/resources/${resource.name}/actions/${action.name}"
            ) {
                header(HttpHeaders.Authorization, "Bearer $rootUserToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(UpdateActionRequest(description = "new description")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                response.contentType()
            )

            val responseBody = gson.fromJson(response.bodyAsText(), Action::class.java)
            assertEquals(action.name, responseBody.name)
            assertEquals(resource.name, responseBody.resourceName)
            assertEquals(newDescription, responseBody.description)

            deleteOrganization(organization.id)
        }
    }
}

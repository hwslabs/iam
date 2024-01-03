package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.CreateSubOrganizationRequest
import com.hypto.iam.server.models.CreateSubOrganizationResponse
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.SubOrganizationService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * SubOrganizations provides hierarchy structure for organizations. Under an organizations, there can be multiple sub
 * organizations and resources like users, groups, roles can be attached to sub organizations to
 * control access.
 *
 * This controller class provides APIs to create, update, delete and get sub organizations under an org.
 * NOTE: Currently just one level of sub-organization can be created under an organization.
 */

fun Route.subOrganizationsApi() {
    val service: SubOrganizationService by inject()
    val gson: Gson by inject()

    route("/organizations/{organization_id}/sub_organizations") {
        withPermission(
            "listSubOrganizations",
            getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1)
        ) {
            get {
                val organizationId = call.parameters["organization_id"] ?: throw IllegalArgumentException(
                    "organization_id is required"
                )
                val nextToken = call.request.queryParameters["next_token"]
                val pageSize = call.request.queryParameters["page_size"]
                val sortOrder = call.request.queryParameters["sortOrder"]

                val paginationContext = PaginationContext.from(
                    nextToken,
                    pageSize?.toInt(),
                    sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
                )
                val response = service.listSubOrganizations(organizationId, paginationContext)
                call.respondText(
                    text = gson.toJson(response),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }

        withPermission(
            "createSubOrganization",
            getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1)
        ) {
            post {
                val organizationId = call.parameters["organization_id"] ?: throw IllegalArgumentException(
                    "organization_id is required"
                )
                val request = call.receive<CreateSubOrganizationRequest>().validate()
                val subOrganization = service.createSubOrganization(
                    organizationId = organizationId,
                    id = request.id,
                    name = request.name,
                    description = request.description
                )
                call.respondText(
                    text = gson.toJson(CreateSubOrganizationResponse(subOrganization)),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.Created
                )
            }
        }
    }

    route("/organizations/{org_id}/sub_organizations/{id}") {
        withPermission(
            "deleteSubOrganization",
            getResourceHrnFunc(
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ) {
            delete {
                val orgId = call.parameters["org_id"]!!
                val id = call.parameters["id"]!!
                val response = service.deleteSubOrganization(orgId, id)
                call.respondText(
                    text = gson.toJson(response),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }

        withPermission(
            "getSubOrganization",
            getResourceHrnFunc(
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ) {
            get {
                val id = call.parameters["id"]!!
                val orgId = call.parameters["org_id"]!!
                val response = service.getSubOrganization(orgId, id)
                call.respondText(
                    text = gson.toJson(response),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }

        withPermission(
            "updateSubOrganization",
            getResourceHrnFunc(
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationIdIndex = 3
            )
        ) {
            patch {
                val id = call.parameters["id"]!!
                val orgId = call.parameters["org_id"]!!
                val request = call.receive<UpdateOrganizationRequest>().validate()
                val response =
                    service.updateSubOrganization(
                        organizationId = orgId,
                        id = id,
                        updatedName = request.name,
                        updatedDescription = request.description,
                    )
                call.respondText(
                    text = gson.toJson(response),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK
                )
            }
        }
    }
}

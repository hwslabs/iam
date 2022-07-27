package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdateActionRequest
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.ActionService
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
import org.koin.ktor.ext.inject

fun Route.actionApi() {
    val actionService: ActionService by inject()
    val gson: Gson by inject()

    withPermission(
        "createAction",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        post("/organizations/{organization_id}/resources/{resource_name}/actions") {
            val organizationId = call.parameters["organization_id"]
            val resourceName = call.parameters["resource_name"]
            val request = call.receive<CreateActionRequest>().validate()

            val response =
                actionService.createAction(organizationId!!, resourceName!!, request.name, request.description ?: "")

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }
    }

    withPermission(
        "listAction",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        get("/organizations/{organization_id}/resources/{resource_name}/actions") {
            val organizationId = call.parameters["organization_id"]
            val resourceName = call.parameters["resource_name"]
            val nextToken = call.request.queryParameters["next_token"]
            val pageSize = call.request.queryParameters["page_size"]
            val sortOrder = call.request.queryParameters["sort_order"]

            val context = PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
            )

            val response = actionService.listActions(organizationId!!, resourceName!!, context)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission(
        "getAction",
        getResourceHrnFunc(resourceNameIndex = 4, resourceInstanceIndex = 5, organizationIdIndex = 1)
    ) {
        get("/organizations/{organization_id}/resources/{resource_name}/actions/{action_name}") {
            val organizationId = call.parameters["organization_id"]
            val resourceName = call.parameters["resource_name"]
            val actionName = call.parameters["action_name"]

            val response = actionService.getAction(organizationId!!, resourceName!!, actionName!!)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission(
        "updateAction",
        getResourceHrnFunc(resourceNameIndex = 4, resourceInstanceIndex = 5, organizationIdIndex = 1)
    ) {
        patch("/organizations/{organization_id}/resources/{resource_name}/actions/{action_name}") {
            val organizationId = call.parameters["organization_id"]
            val resourceName = call.parameters["resource_name"]
            val actionName = call.parameters["action_name"]
            val request = call.receive<UpdateActionRequest>().validate()

            val response =
                actionService.updateAction(organizationId!!, resourceName!!, actionName!!, request.description ?: "")

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission(
        "deleteAction",
        getResourceHrnFunc(resourceNameIndex = 4, resourceInstanceIndex = 5, organizationIdIndex = 1)
    ) {
        delete("/organizations/{organization_id}/resources/{resource_name}/actions/{action_name}") {
            val organizationId = call.parameters["organization_id"]
            val resourceName = call.parameters["resource_name"]
            val actionName = call.parameters["action_name"]

            val response = actionService.deleteAction(organizationId!!, resourceName!!, actionName!!)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}

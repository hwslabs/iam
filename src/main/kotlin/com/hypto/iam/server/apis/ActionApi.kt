package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdateActionRequest
import com.hypto.iam.server.service.ActionServiceImpl
import com.hypto.iam.server.validators.validate
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import org.koin.ktor.ext.inject

fun Route.actionApi() {
    val actionService: ActionServiceImpl by inject()
    val gson: Gson by inject()

    post("/organizations/{organization_id}/resources/{resource_name}/actions") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceName = call.parameters["resource_name"]
            ?: throw IllegalArgumentException("Required resourceName to create an action")
        val request = call.receive<CreateActionRequest>().validate()

        val response = actionService.createAction(organizationId, resourceName, request.name, request.description ?: "")

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created
        )
    }

    get("/organizations/{organization_id}/resources/{resource_name}/actions") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceName = call.parameters["resource_name"]
            ?: throw IllegalArgumentException("Required resourceName to create an action")
        val nextToken = call.request.queryParameters["next_token"]
        val pageSize = call.request.queryParameters["page_size"]
        val sortOrder = call.request.queryParameters["sort_order"]

        val context = PaginationContext.from(
            nextToken,
            pageSize?.toInt(),
            sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
        )

        val response = actionService.listActions(organizationId, resourceName, context)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    get("/organizations/{organization_id}/resources/{resource_name}/actions/{action_name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceName = call.parameters["resource_name"]
            ?: throw IllegalArgumentException("Required resourceName to get an action")
        val actionName = call.parameters["action_name"]
            ?: throw IllegalArgumentException("Required actionName to get an action")

        val response = actionService.getAction(organizationId, resourceName, actionName)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    patch("/organizations/{organization_id}/resources/{resource_name}/actions/{action_name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceName = call.parameters["resource_name"]
            ?: throw IllegalArgumentException("Required resourceName to update an action")
        val actionName = call.parameters["action_name"]
            ?: throw IllegalArgumentException("Required actionName to update an action")
        val request = call.receive<UpdateActionRequest>().validate()

        val response =
            actionService.updateAction(organizationId, resourceName, actionName, request.description)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    delete("/organizations/{organization_id}/resources/{resource_name}/actions/{action_name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceName = call.parameters["resource_name"]
            ?: throw IllegalArgumentException("Required resourceName to delete an action")
        val actionName = call.parameters["action_name"]
            ?: throw IllegalArgumentException("Required actionName to delete an action")

        val response = actionService.deleteAction(organizationId, resourceName, actionName)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

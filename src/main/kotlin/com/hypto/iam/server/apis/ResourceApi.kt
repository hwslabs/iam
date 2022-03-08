package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdateResourceRequest
import com.hypto.iam.server.service.ResourceService
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

fun Route.resourceApi() {

    val resourceService: ResourceService by inject()
    val gson: Gson by inject()

    post("/organizations/{organization_id}/resources") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val request = call.receive<CreateResourceRequest>().validate()

        val resource =
            resourceService.createResource(organizationId, request.name, request.description ?: "")
        call.respondText(
            text = gson.toJson(resource),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created
        )
    }

    get("/organizations/{organization_id}/resources") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to get user")
        val nextToken = call.request.queryParameters["next_token"]
        val pageSize = call.request.queryParameters["page_size"]
        val sortOrder = call.request.queryParameters["sort_order"]

        val context = PaginationContext.from(
            nextToken,
            pageSize?.toInt(),
            sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
        )

        val response = resourceService.listResources(organizationId, context)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    delete("/organizations/{organization_id}/resources/{name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to delete a resource")

        val response = resourceService.deleteResource(organizationId, name)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    get("/organizations/{organization_id}/resources/{name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to get the resource")

        val response = resourceService.getResource(organizationId, name)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    patch("/organizations/{organization_id}/resources/{name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to update a resource")
        val request = call.receive<UpdateResourceRequest>().validate()

        val response = resourceService.updateResource(organizationId, name, request.description ?: "")

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateResourceTypeRequest
import com.hypto.iam.server.models.UpdateResourceTypeRequest
import com.hypto.iam.server.service.ResourceTypeService
import com.hypto.iam.server.validators.validate
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import org.koin.ktor.ext.inject

@KtorExperimentalLocationsAPI
fun Route.resourceTypeApi() {

    val resourceTypeService: ResourceTypeService by inject()
    val gson: Gson by inject()

    post("/organizations/{organization_id}/resource_types") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val request = call.receive<CreateResourceTypeRequest>().validate()

        val resourceType =
            resourceTypeService.createResourceType(organizationId, request.name, request.description ?: "")
        call.respondText(
            text = gson.toJson(resourceType),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created
        )
    }

    delete("/organizations/{organization_id}/resource_types/{name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to delete a resource_type")

        val response = resourceTypeService.deleteResourceType(organizationId, name)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    get("/organizations/{organization_id}/resource_types/{name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to get the resource_type")

        val response = resourceTypeService.getResourceType(organizationId, name)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    patch("/organizations/{organization_id}/resource_types/{name}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to update a resource_type")
        val request = call.receive<UpdateResourceTypeRequest>().validate()

        val response = resourceTypeService.updateResourceType(organizationId, name, request.description ?: "")

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

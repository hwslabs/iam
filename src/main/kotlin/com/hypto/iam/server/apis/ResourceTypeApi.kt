@file:Suppress("ThrowsCount", "UnusedPrivateMember")
package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.UpdateResourceRequest
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import org.koin.ktor.ext.inject

@KtorExperimentalLocationsAPI
fun Route.resourceTypeApi() {

    val gson: Gson by inject()

    post("/organizations/{organization_id}/resource_types") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val request = call.receive<CreateResourceRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    delete("/organizations/{organization_id}/resource_types/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to delete a resource_type")
        call.respond(HttpStatusCode.NotImplemented)
    }

    get("/organizations/{organization_id}/resource_types/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to get the resource_type")
        call.respond(HttpStatusCode.NotImplemented)
    }

    patch("/organizations/{organization_id}/resource_types/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to update a resource_type")
        val request = call.receive<UpdateResourceRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }
}

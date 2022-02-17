@file:Suppress("ThrowsCount", "UnusedPrivateMember")
package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.UpdateActionRequest
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
fun Route.actionApi() {

    val gson: Gson by inject()

    post("/organizations/{organization_id}/resources/{resourceId}/action") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceId = call.parameters["resourceId"]
            ?: throw IllegalArgumentException("Required resourceId to create an action")
        val request = call.receive<CreateActionRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    delete("/organizations/{organization_id}/resources/{resourceId}/action/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceId = call.parameters["resourceId"]
            ?: throw IllegalArgumentException("Required resourceId to delete an action")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to delete a resource_type")
        call.respond(HttpStatusCode.NotImplemented)
    }

    get("/organizations/{organization_id}/resources/{resourceId}/action/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceId = call.parameters["resourceId"]
            ?: throw IllegalArgumentException("Required resourceId to get an action")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to get the resource_type")
        call.respond(HttpStatusCode.NotImplemented)
    }

    patch("/organizations/{organization_id}/resources/{resourceId}/action/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val resourceId = call.parameters["resourceId"]
            ?: throw IllegalArgumentException("Required resourceId to update an action")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to update a resource_type")
        val request = call.receive<UpdateActionRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }
}

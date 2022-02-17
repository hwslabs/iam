@file:Suppress("ThrowsCount", "UnusedPrivateMember")
package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import org.koin.ktor.ext.inject

fun Route.credentialApi() {
    val gson: Gson by inject()

    post("/organizations/{organization_id}/users/{userId}/credential") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val request = call.receive<CreateCredentialRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    delete("/organizations/{organization_id}/users/{userId}/credential/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("id required")
        call.respond(HttpStatusCode.NotImplemented)
    }

    get("/organizations/{organization_id}/users/{userId}/credential/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("id required")
        call.respond(HttpStatusCode.NotImplemented)
    }

    patch("/organizations/{organization_id}/users/{userId}/credential/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("id required")
        val request = call.receive<UpdateCredentialRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }
}

@file:Suppress("ThrowsCount", "UnusedPrivateMember")
package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.UpdatePolicyRequest
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

fun Route.policyApi() {

    val gson: Gson by inject()

    post("/organizations/{organization_id}/policies") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val request = call.receive<CreatePolicyRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    delete("/organizations/{organization_id}/policies/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to delete a policy")
        call.respond(HttpStatusCode.NotImplemented)
    }

    get("/organizations/{organization_id}/policies/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to get the policy details")
        call.respond(HttpStatusCode.NotImplemented)
    }

    get("/organizations/{organization_id}/users/{user_id}/policies") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required user id to list policies")
        call.respond(HttpStatusCode.NotImplemented)
    }

    patch("/organizations/{organization_id}/policies/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("organization_id required")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to get the policy details")
        val request = call.receive<UpdatePolicyRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }
}

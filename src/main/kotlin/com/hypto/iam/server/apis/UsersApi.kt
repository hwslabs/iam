@file:Suppress("ThrowsCount", "UnusedPrivateMember")
package com.hypto.iam.server.apis

import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.UpdateUserRequest
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put

fun Route.usersApi() {

    put("/organizations/{organization_id}/users/{id}/attach_policies") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to attach policies")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to attach policies")
        val request = call.receive<PolicyAssociationRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    post("/organizations/{organization_id}/users") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to create user")
        val request = call.receive<CreateUserRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    delete("/organizations/{organization_id}/users/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to delete user")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to delete a user")
        call.respond(HttpStatusCode.NotImplemented)
    }

    put("/organizations/{organization_id}/users/{id}/detach_policies") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to detach policies")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to detach policies")
        val request = call.receive<PolicyAssociationRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }

    get("/organizations/{organization_id}/users/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to get user")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to get the user")
        call.respond(HttpStatusCode.NotImplemented)
    }

    patch("/organizations/{organization_id}/users/{id}") {
        val organizationId = call.parameters["organization_id"]
            ?: throw IllegalArgumentException("Required organization_id to update user")
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Required id to update the user")
        val request = call.receive<UpdateUserRequest>() // .validate()
        call.respond(HttpStatusCode.NotImplemented)
    }
}

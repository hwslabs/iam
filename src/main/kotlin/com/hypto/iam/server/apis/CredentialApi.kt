@file:Suppress("ThrowsCount")
package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.CredentialService
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
import java.util.UUID
import org.koin.ktor.ext.inject

fun Route.credentialApi() {
    val gson: Gson by inject()
    val credentialService: CredentialService by inject()

    withPermission("createCredential") {
        post("/organizations/{organization_id}/users/{userId}/credentials") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
            val request = call.receive<CreateCredentialRequest>().validate()

            val response = credentialService.createCredential(organizationId, userId, request.validUntil)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }
    }

    withPermission("deleteCredential") {
        delete("/organizations/{organization_id}/users/{userId}/credentials/{id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
            val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("id required"))

            val response = credentialService.deleteCredential(organizationId, userId, id)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("getCredential") {
        get("/organizations/{organization_id}/users/{userId}/credentials/{id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
            val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("id required"))

            val credential = credentialService.getCredentialWithoutSecret(organizationId, userId, id)

            call.respondText(
                text = gson.toJson(credential),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("updateCredential") {
        patch("/organizations/{organization_id}/users/{userId}/credentials/{id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val userId = call.parameters["userId"] ?: throw IllegalArgumentException("userId required")
            val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("id required"))
            val request = call.receive<UpdateCredentialRequest>().validate()

            val response = credentialService.updateCredentialAndGetWithoutSecret(
                organizationId, userId, id, request.status, request.validUntil
            )

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}

@file:Suppress("ThrowsCount", "LongMethod")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdatePolicyRequest
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.PolicyService
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

fun Route.policyApi() {

    val policyService: PolicyService by inject()
    val gson: Gson by inject()

    withPermission("createPolicy") {
        post("/organizations/{organization_id}/policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val request = call.receive<CreatePolicyRequest>().validate()

            val policy = policyService.createPolicy(organizationId, request.name, request.statements)

            call.respondText(
                text = gson.toJson(policy),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }
    }

    withPermission("listPolicy") {
        get("/organizations/{organization_id}/policies") {
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

            val response = policyService.listPolicies(organizationId, context)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("deletePolicy") {
        delete("/organizations/{organization_id}/policies/{name}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to delete a policy")

            val response = policyService.deletePolicy(organizationId, name)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("getPolicy") {
        get("/organizations/{organization_id}/policies/{name}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val name =
                call.parameters["name"] ?: throw IllegalArgumentException("Required name to get the policy details")

            val response = policyService.getPolicy(organizationId, name)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("getUserPolicy") {
        get("/organizations/{organization_id}/users/{user_id}/policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val userId =
                call.parameters["user_id"] ?: throw IllegalArgumentException("Required user id to list policies")

            val nextToken = call.request.queryParameters["next_token"]
            val pageSize = call.request.queryParameters["page_size"]
            val sortOrder = call.request.queryParameters["sort_order"]

            val context = PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
            )

            val response = policyService.getPoliciesByUser(organizationId, userId, context)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("updatePolicy") {
        patch("/organizations/{organization_id}/policies/{name}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("organization_id required")
            val name = call.parameters["name"]
                ?: throw IllegalArgumentException("Required name to update the policy details")
            val request = call.receive<UpdatePolicyRequest>().validate()

            val response = policyService.updatePolicy(organizationId, name, request.statements)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}

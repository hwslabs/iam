@file:Suppress("ThrowsCount", "LongMethod")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.UpdatePolicyRequest
import com.hypto.iam.server.security.AuthorizationException
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.PolicyService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.policyApi() {
    val policyService: PolicyService by inject()
    val gson: Gson by inject()

    withPermission(
        "createPolicy",
        getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1),
    ) {
        post("/organizations/{organization_id}/policies") {
            val organizationId =
                call.parameters["organization_id"]
                    ?: throw IllegalArgumentException("organization_id required")
            val request = call.receive<CreatePolicyRequest>().validate()

            val principal = context.principal<UserPrincipal>()!!
            if (principal.hrn.organization != organizationId) {
                throw AuthorizationException("Cross organization policy creation is not supported")
            }

            val policy =
                policyService.createPolicy(organizationId, request.name, request.description, request.statements)

            call.respondText(
                text = gson.toJson(policy),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created,
            )
        }
    }

    withPermission(
        "listPolicy",
        getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1),
    ) {
        get("/organizations/{organization_id}/policies") {
            val organizationId =
                call.parameters["organization_id"]
                    ?: throw IllegalArgumentException("Required organization_id to get user")
            val nextToken = call.request.queryParameters["nextToken"]
            val pageSize = call.request.queryParameters["pageSize"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val context =
                PaginationContext.from(
                    nextToken,
                    pageSize?.toInt(),
                    sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) },
                )

            val response = policyService.listPolicies(organizationId, context)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }

    withPermission(
        "deletePolicy",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1),
    ) {
        delete("/organizations/{organization_id}/policies/{name}") {
            val organizationId =
                call.parameters["organization_id"]
                    ?: throw IllegalArgumentException("organization_id required")
            val name = call.parameters["name"] ?: throw IllegalArgumentException("Required name to delete a policy")

            val response = policyService.deletePolicy(organizationId, name)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }

    withPermission(
        "getPolicy",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1),
    ) {
        get("/organizations/{organization_id}/policies/{name}") {
            val organizationId =
                call.parameters["organization_id"]
                    ?: throw IllegalArgumentException("organization_id required")
            val name =
                call.parameters["name"] ?: throw IllegalArgumentException("Required name to get the policy details")

            val response = policyService.getPolicy(organizationId, name)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }

    withPermission(
        "getUserPolicy",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1),
    ) {
        get("/organizations/{organization_id}/users/{user_id}/policies") {
            val organizationId =
                call.parameters["organization_id"]
                    ?: throw IllegalArgumentException("organization_id required")
            val userId =
                call.parameters["user_id"] ?: throw IllegalArgumentException("Required user id to list policies")

            val nextToken = call.request.queryParameters["nextToken"]
            val pageSize = call.request.queryParameters["pageSize"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val context =
                PaginationContext.from(
                    nextToken,
                    pageSize?.toInt(),
                    sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) },
                )

            val response = policyService.getPoliciesByUser(organizationId, userId, context)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }

    withPermission(
        "updatePolicy",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1),
    ) {
        patch("/organizations/{organization_id}/policies/{name}") {
            val organizationId =
                call.parameters["organization_id"]
                    ?: throw IllegalArgumentException("organization_id required")
            val name =
                call.parameters["name"]
                    ?: throw IllegalArgumentException("Required name to update the policy details")
            val request = call.receive<UpdatePolicyRequest>().validate()

            val response = policyService.updatePolicy(organizationId, name, request)

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }
}

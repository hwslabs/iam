@file:Suppress("ThrowsCount", "UnusedPrivateMember")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.idp.PasswordCredentials
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.UserPaginatedResponse
import com.hypto.iam.server.plugins.inject
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.UserPolicyService
import com.hypto.iam.server.service.UsersService
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
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

fun Route.usersApi() {
    val userPolicyService: UserPolicyService by inject()
    val gson: Gson by inject()
    val hrnFactory: HrnFactory by inject()
    val usersService: UsersService by inject()

    // **** User management apis ****//

    // Create user
    withPermission("createUser") {
        post("/organizations/{organization_id}/users") {
            val organizationId = call.parameters["organization_id"]!!
            val request = call.receive<CreateUserRequest>().validate()
            val passwordCredentials = PasswordCredentials(
                userName = request.username,
                email = request.email, phoneNumber = request.phone ?: "", password = request.passwordHash
            )
            val user = usersService.createUser(
                organizationId = organizationId,
                credentials = passwordCredentials,
                createdBy = call.principal<UserPrincipal>()?.hrnStr,
                verified = false
            )
            call.respondText(
                text = gson.toJson(user),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }
    }

    // Get user
    withPermission("getUser") {
        get("/organizations/{organization_id}/users/{id}") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["id"]!!
            val user = usersService.getUser(organizationId, userId)
            call.respondText(
                text = gson.toJson(user),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // List user
    withPermission("listUser") {
        get("/organizations/{organization_id}/users") {
            val organizationId = call.parameters["organization_id"]!!
            val nextToken = call.request.queryParameters["next_token"]
            val pageSize = call.request.queryParameters["page_size"]
            val (users, token, options) = usersService.listUsers(organizationId, nextToken, pageSize?.toInt())
            val response = UserPaginatedResponse(data = users, nextToken = token, context = options)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // Delete user
    withPermission("deleteUser") {
        delete("/organizations/{organization_id}/users/{user_id}") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["user_id"]!!
            val response = usersService.deleteUser(organizationId, userId)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // Update user
    withPermission("updateUser") {
        patch("/organizations/{organization_id}/users/{user_id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to update user")
            val userId = call.parameters["user_id"]!!
            val request = call.receive<UpdateUserRequest>().validate()
            val user =
                usersService.updateUser(
                    organizationId,
                    userId,
                    request.email ?: "",
                    request.phone ?: "",
                    request.status
                )
            call.respondText(
                text = gson.toJson(user),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // **** User policy management apis ****//

    // Detach policy
    withPermission("detachPolicies") {
        patch("/organizations/{organization_id}/users/{user_id}/detach_policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to detach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to detach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()

            val response = userPolicyService.detachPoliciesToUser(
                ResourceHrn(organizationId, "", IamResources.USER, userId),
                request.policies.map { hrnFactory.getHrn(it) }
            )
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // Attach policy
    withPermission("attachPolicies") {
        patch("/organizations/{organization_id}/users/{user_id}/attach_policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to attach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to attach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()
            val response = userPolicyService.attachPoliciesToUser(
                ResourceHrn(organizationId, "", IamResources.USER, userId),
                request.policies.map { hrnFactory.getHrn(it) }
            )
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}

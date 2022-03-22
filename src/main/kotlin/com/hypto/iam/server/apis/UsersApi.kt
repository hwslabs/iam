@file:Suppress("ThrowsCount", "UnusedPrivateMember")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.UserPolicyService
import com.hypto.iam.server.service.UsersService
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.validators.validate
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import org.koin.ktor.ext.inject

fun Route.usersApi() {

    val userPolicyService: UserPolicyService by inject()
    val gson: Gson by inject()
    val hrnFactory: HrnFactory by inject()
    val usersService: UsersService by inject()

    withPermission("attachPolicy") {
        put("/organizations/{organization_id}/users/{user_id}/attach_policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to attach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to attach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()

            val response = userPolicyService.attachPoliciesToUser(
                ResourceHrn(organizationId, "", IamResourceTypes.USER, userId),
                request.policies.map { hrnFactory.getHrn(it) }
            )

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("createUser") {
        post("/organizations/{organization_id}/users") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to create user")
            val request = call.receive<CreateUserRequest>() // .validate()
            val principal = context.principal<UserPrincipal>()!!
            val user = usersService.createUser(
                organizationId = organizationId, userName = request.username,
                password = request.passwordHash,
                email = request.email,
                createdBy = principal.hrn,
                userType = User.UserType.valueOf(request.userType.toString()),
                status = User.Status.valueOf(request.status.toString()),
                phone = request.phone
            )
            call.respondText(
                text = gson.toJson(user),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("listUser") {
        get("/organizations/{organization_id}/users") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to get user")
            val nextToken = call.request.queryParameters["next_token"]
            val pageSize = call.request.queryParameters["page_size"]
            val sortOrder = call.request.queryParameters["sort_order"]

            call.respond(HttpStatusCode.NotImplemented)
        }
    }

    withPermission("deletePermission") {
        delete("/organizations/{organization_id}/users/{user_id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to delete user")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to delete a user")
            call.respond(HttpStatusCode.NotImplemented)
        }
    }

    withPermission("detachPolicy") {
        put("/organizations/{organization_id}/users/{user_id}/detach_policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to detach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to detach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()

            val response = userPolicyService.detachPoliciesToUser(
                ResourceHrn(organizationId, "", IamResourceTypes.USER, userId),
                request.policies.map { hrnFactory.getHrn(it) }
            )

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission("updateUser") {
        patch("/organizations/{organization_id}/users/{user_id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to update user")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to update the user")
            val request = call.receive<UpdateUserRequest>() // .validate()
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}

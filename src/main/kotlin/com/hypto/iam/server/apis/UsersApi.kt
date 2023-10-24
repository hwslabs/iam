@file:Suppress("ThrowsCount", "UnusedPrivateMember")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.models.ChangeUserPasswordRequest
import com.hypto.iam.server.models.CreateUserPasswordRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.CreateUserResponse
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.ResetPasswordRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.IamPrincipal
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.service.PrincipalPolicyService
import com.hypto.iam.server.service.TokenService
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.service.UsersService
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.validators.InviteMetadata
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

fun Route.createUsersApi() {
    val gson: Gson by inject()
    val usersService: UsersService by inject()
    val passcodeRepo: PasscodeRepo by inject()
    val passcodeService: PasscodeService by inject()
    val idGenerator: ApplicationIdUtil.Generator by inject()
    val tokenService: TokenService by inject()
    val userAuthRepo: UserAuthRepo by inject()

    // **** Create User api ****//

    // Create user
    withPermission(
        "createUser",
        getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1)
    ) {
        post("/organizations/{organization_id}/users") {
            val principal = context.principal<IamPrincipal>() ?: throw AuthenticationException("User not authenticated")
            val organizationId = call.parameters["organization_id"]!!
            val request = call.receive<CreateUserRequest>().validate()

            var verified: Boolean = request.verified ?: false
            var loginAccess: Boolean = request.loginAccess ?: false
            var policies: List<String>? = null

            if (principal.tokenCredential.type == TokenType.PASSCODE) {
                val passcode = passcodeRepo.getValidPasscodeById(
                    principal.tokenCredential.value!!,
                    VerifyEmailRequest.Purpose.invite,
                    organizationId = organizationId
                ) ?: throw AuthenticationException("Invalid passcode")
                require(passcode.email == request.email) { "Email in passcode does not match email in request" }
                verified = true
                loginAccess = true
                policies = InviteMetadata(passcodeService.decryptMetadata(passcode.metadata!!)).policies
            }

            val username = idGenerator.username()
            val user = usersService.createUser(
                organizationId = organizationId,
                username = username,
                preferredUsername = request.preferredUsername,
                name = request.name,
                email = request.email,
                phoneNumber = request.phone ?: "",
                password = request.password,
                createdBy = call.principal<UserPrincipal>()?.hrnStr,
                verified = verified,
                loginAccess = loginAccess,
                policies = policies
            )
            userAuthRepo.create(
                hrn = user.hrn,
                providerName = TokenServiceImpl.ISSUER,
                authMetadata = null
            )

            val token = tokenService.generateJwtToken(ResourceHrn(user.hrn))
            call.respondText(
                text = gson.toJson(CreateUserResponse(user, token.token)),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created
            )
        }
    }
}

fun Route.usersApi() {
    val principalPolicyService: PrincipalPolicyService by inject()
    val gson: Gson by inject()
    val hrnFactory: HrnFactory by inject()
    val usersService: UsersService by inject()
    val appConfig: AppConfig by inject()

    // **** User Management api ****//

    // Get user
    withPermission(
        "getUser",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
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
    withPermission(
        "listUser",
        getResourceHrnFunc(resourceNameIndex = 0, resourceInstanceIndex = 1, organizationIdIndex = 1)
    ) {
        get("/organizations/{organization_id}/users") {
            val organizationId = call.parameters["organization_id"]!!
            val nextToken = call.request.queryParameters["next_token"]
            val pageSize = call.request.queryParameters["page_size"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val paginationContext = PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
            )

            val response = usersService.listUsers(organizationId, paginationContext)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // Delete user
    withPermission(
        "deleteUser",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
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
    withPermission(
        "updateUser",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        patch("/organizations/{organization_id}/users/{user_id}") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to update user")
            val userId = call.parameters["user_id"]!!
            val request = call.receive<UpdateUserRequest>().validate()
            val user =
                usersService.updateUser(
                    organizationId,
                    userId,
                    request.name,
                    request.phone ?: "",
                    request.status,
                    request.verified
                )
            call.respondText(
                text = gson.toJson(user),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission(
        "changePassword",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        post("/organizations/{organization_id}/users/{user_id}/change_password") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["user_id"]!!
            val request = call.receive<ChangeUserPasswordRequest>().validate()
            val response = usersService.changeUserPassword(
                organizationId,
                userId,
                request.oldPassword,
                request.newPassword
            )
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    withPermission(
        "createPassword",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        post("/organizations/{organization_id}/users/{user_id}/create_password") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["user_id"]!!
            val request = call.receive<CreateUserPasswordRequest>().validate()
            val response = usersService.createUserPassword(
                organizationId,
                userId,
                request.password
            )

            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    // **** User policy management apis ****//

    // Detach policy
    withPermission(
        "detachPolicies",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        patch("/organizations/{organization_id}/users/{user_id}/detach_policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to detach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to detach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()

            val response = principalPolicyService.detachPoliciesToUser(
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
    withPermission(
        "attachPolicies",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        patch("/organizations/{organization_id}/users/{user_id}/attach_policies") {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to attach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to attach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()
            val response = principalPolicyService.attachPoliciesToUser(
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

fun Route.resetPasswordApi() {
    val gson: Gson by inject()
    val usersService: UsersService by inject()

    post("/organizations/{organization_id}/users/resetPassword") {
        val organizationId = call.parameters["organization_id"]
        val passcodeStr = call.principal<ApiPrincipal>()!!.tokenCredential.value!!
        val request = call.receive<ResetPasswordRequest>().validate()
        val user = usersService.getUserByEmail(organizationId!!, request.email)
        val response = usersService.setUserPassword(organizationId, user, request.password)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

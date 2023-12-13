@file:Suppress("ThrowsCount", "UnusedPrivateMember")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.delete
import com.hypto.iam.server.extensions.get
import com.hypto.iam.server.extensions.patch
import com.hypto.iam.server.extensions.post
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
import com.hypto.iam.server.security.HrnTemplateInput
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users",
                    resourceNameIndex = 0,
                    resourceInstanceIndex = 1,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        post(
            "/organizations/{organization_id}/users",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users"
        ) {
            val principal = context.principal<IamPrincipal>() ?: throw AuthenticationException("User not authenticated")
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
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
                subOrganizationId = subOrganizationId,
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{id}",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users" +
                        "/{id}",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        get(
            "/organizations/{organization_id}/users/{id}",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{id}",
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
            val userId = call.parameters["id"]!!
            val user = usersService.getUser(organizationId, subOrganizationId, userId)
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users",
                    resourceNameIndex = 0,
                    resourceInstanceIndex = 1,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        get(
            "/organizations/{organization_id}/users",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users"
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
            val nextToken = call.request.queryParameters["next_token"]
            val pageSize = call.request.queryParameters["page_size"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val paginationContext = PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
            )

            val response = usersService.listUsers(organizationId, subOrganizationId, paginationContext)
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{user_id}",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        delete(
            "/organizations/{organization_id}/users/{user_id}",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}"
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
            val userId = call.parameters["user_id"]!!
            val response = usersService.deleteUser(organizationId, subOrganizationId, userId)
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{user_id}",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        patch(
            "/organizations/{organization_id}/users/{user_id}",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}"
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
            val userId = call.parameters["user_id"]!!
            val request = call.receive<UpdateUserRequest>().validate()
            val user =
                usersService.updateUser(
                    organizationId,
                    subOrganizationId,
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{user_id}/change_password",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}" +
                        "/change_password",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        post(
            "/organizations/{organization_id}/users/{user_id}/change_password",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}/change_password"
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
            val userId = call.parameters["user_id"]!!
            val request = call.receive<ChangeUserPasswordRequest>().validate()
            val response = usersService.changeUserPassword(
                organizationId,
                subOrganizationId,
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{user_id}/create_password",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}" +
                        "/create_password",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )

    ) {
        post(
            "/organizations/{organization_id}/users/{user_id}/create_password",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}/create_password"
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
            val userId = call.parameters["user_id"]!!
            val request = call.receive<CreateUserPasswordRequest>().validate()
            val response = usersService.createUserPassword(
                organizationId,
                subOrganizationId,
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{user_id}/detach_policies",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}" +
                        "/detach_policies",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        patch(
            "/organizations/{organization_id}/users/{user_id}/detach_policies",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}/detach_policies"
        ) {
            val organizationId = call.parameters["organization_id"]!!
            val subOrganizationId = call.parameters["sub_organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to detach policies")
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to detach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()

            val response = principalPolicyService.detachPoliciesToUser(
                ResourceHrn(organizationId, subOrganizationId ?: "", IamResources.USER, userId),
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
        getResourceHrnFunc(
            listOf(
                HrnTemplateInput(
                    "/organizations/{organization_id}/users/{user_id}/attach_policies",
                    resourceNameIndex = 2,
                    resourceInstanceIndex = 3,
                    organizationIdIndex = 1
                ),
                HrnTemplateInput(
                    "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}" +
                        "/attach_policies",
                    resourceNameIndex = 4,
                    resourceInstanceIndex = 5,
                    organizationIdIndex = 1,
                    subOrganizationIdIndex = 3
                )
            )
        )
    ) {
        patch(
            "/organizations/{organization_id}/users/{user_id}/attach_policies",
            "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/{user_id}/attach_policies"
        ) {
            val organizationId = call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to attach policies")
            val subOrganizationId = call.parameters["sub_organization_id"]
            val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to attach policies")
            val request = call.receive<PolicyAssociationRequest>().validate()
            val response = principalPolicyService.attachPoliciesToUser(
                ResourceHrn(organizationId, subOrganizationId ?: "", IamResources.USER, userId),
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

    post(
        "/organizations/{organization_id}/users/resetPassword",
        "/organizations/{organization_id}/sub_organizations/{sub_organization_id}/users/resetPassword"
    ) {
        val organizationId = call.parameters["organization_id"]
        val subOrganizationId = call.parameters["sub_organization_id"]
        val passcodeStr = call.principal<ApiPrincipal>()!!.tokenCredential.value!!
        val request = call.receive<ResetPasswordRequest>().validate()
        val user = usersService.getUserByEmail(organizationId!!, subOrganizationId, request.email)
        val response = usersService.setUserPassword(organizationId, subOrganizationId, user, request.password)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

@file:Suppress("ThrowsCount", "UnusedPrivateMember")

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.authProviders.AuthProviderRegistry
import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.RouteOption
import com.hypto.iam.server.extensions.deleteWithPermission
import com.hypto.iam.server.extensions.getWithPermission
import com.hypto.iam.server.extensions.patchWithPermission
import com.hypto.iam.server.extensions.post
import com.hypto.iam.server.extensions.postWithPermission
import com.hypto.iam.server.models.ChangeUserPasswordRequest
import com.hypto.iam.server.models.CreateUserPasswordRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.CreateUserResponse
import com.hypto.iam.server.models.LinkUserRequest
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.ResetPasswordRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.IamPrincipal
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
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
import io.ktor.server.plugins.BadRequestException
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
    postWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users",
                resourceNameIndex = 0,
                resourceInstanceIndex = 1,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "createUser",
    ) {
        val principal = context.principal<IamPrincipal>() ?: throw AuthenticationException("User not authenticated")
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val request = call.receive<CreateUserRequest>().validate()

        var verified: Boolean = request.verified ?: false
        var loginAccess: Boolean = request.loginAccess ?: false
        var policies: List<String>? = null
        var name = request.name
        var email = request.email
        var authMetadata: AuthMetadata? = null
        var issuer: String = TokenServiceImpl.ISSUER

        if (principal.tokenCredential.type == TokenType.PASSCODE) {
            val passcode =
                passcodeRepo.getValidPasscodeById(
                    principal.tokenCredential.value!!,
                    VerifyEmailRequest.Purpose.invite,
                    organizationId = organizationId,
                ) ?: throw AuthenticationException("Invalid passcode")
            require(passcode.email == request.email) { "Email in passcode does not match email in request" }
            verified = true
            loginAccess = true
            policies = InviteMetadata(passcodeService.decryptMetadata(passcode.metadata!!)).policies
            request.issuerName?.let {
                val oAuthUserPrincipal =
                    AuthProviderRegistry.getProvider(it)?.getProfileDetails(TokenCredential(request.issuerToken!!, TokenType.OAUTH))
                        ?: throw BadRequestException("No auth provider found for issuer $it")
                require(request.email == null || request.email == oAuthUserPrincipal.email) {
                    "Email from issuer token is different from the email in request"
                }
                name = name ?: oAuthUserPrincipal.name
                email = oAuthUserPrincipal.email
                authMetadata = oAuthUserPrincipal.metadata
                issuer = it
            }
        }

        val username = idGenerator.username()
        val user =
            usersService.createUser(
                organizationId = organizationId,
                subOrganizationName = subOrganizationName,
                username = username,
                preferredUsername = request.preferredUsername,
                name = name,
                email = email,
                phoneNumber = request.phone ?: "",
                password = request.password,
                createdBy = call.principal<UserPrincipal>()?.hrnStr,
                verified = verified,
                loginAccess = loginAccess,
                status = User.Status.valueOf(request.status.value),
                policies = policies,
            )
        if (loginAccess) {
            userAuthRepo.create(
                hrn = user.hrn,
                providerName = issuer,
                authMetadata = authMetadata?.let { AuthMetadata.toJsonB(it) },
            )
        }
        val token = tokenService.generateJwtToken(ResourceHrn(user.hrn))
        call.respondText(
            text = gson.toJson(CreateUserResponse(user, token.token)),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created,
        )
    }
}

fun Route.usersApi() {
    val principalPolicyService: PrincipalPolicyService by inject()
    val gson: Gson by inject()
    val hrnFactory: HrnFactory by inject()
    val usersService: UsersService by inject()

    // **** User Management api ****//

    // Get user
    getWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{id}",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{id}",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "getUser",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["id"]!!
        val user = usersService.getUser(organizationId, subOrganizationName, userId)
        call.respondText(
            text = gson.toJson(user),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    // List user
    getWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users",
                resourceNameIndex = 0,
                resourceInstanceIndex = 1,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "listUser",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val nextToken = call.request.queryParameters["next_token"]
        val pageSize = call.request.queryParameters["page_size"]
        val sortOrder = call.request.queryParameters["sortOrder"]

        val paginationContext =
            PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) },
            )

        val response = usersService.listUsers(organizationId, subOrganizationName, paginationContext)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    // Delete user
    deleteWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{user_id}",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{user_id}",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "deleteUser",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["user_id"]!!
        val response = usersService.deleteUser(organizationId, subOrganizationName, userId)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    // Update user
    patchWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{user_id}",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{user_id}",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "updateUser",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["user_id"]!!
        val request = call.receive<UpdateUserRequest>().validate()
        val user =
            usersService.updateUser(
                organizationId,
                subOrganizationName,
                userId,
                request.name,
                request.phone ?: "",
                request.status,
                request.verified,
            )
        call.respondText(
            text = gson.toJson(user),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    postWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{user_id}/change_password",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{user_id}/change_password",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "changePassword",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["user_id"]!!
        val request = call.receive<ChangeUserPasswordRequest>().validate()
        val response =
            usersService.changeUserPassword(
                organizationId,
                subOrganizationName,
                userId,
                request.oldPassword,
                request.newPassword,
            )
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    // **** User policy management apis ****//

    // Detach policy
    patchWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{user_id}/detach_policies",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{user_id}/detach_policies",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "detachPolicies",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName =
            call.parameters["sub_organization_name"]
                ?: throw IllegalArgumentException("Required organization_id to detach policies")
        val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to detach policies")
        val request = call.receive<PolicyAssociationRequest>().validate()

        val response =
            principalPolicyService.detachPoliciesToUser(
                ResourceHrn(organizationId, subOrganizationName ?: "", IamResources.USER, userId),
                request.policies.map { hrnFactory.getHrn(it) },
            )
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    // Attach policy
    patchWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{user_id}/attach_policies",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{user_id}/attach_policies",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "attachPolicies",
    ) {
        val organizationId =
            call.parameters["organization_id"]
                ?: throw IllegalArgumentException("Required organization_id to attach policies")
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["user_id"] ?: throw IllegalArgumentException("Required id to attach policies")
        val request = call.receive<PolicyAssociationRequest>().validate()
        val response =
            principalPolicyService.attachPoliciesToUser(
                ResourceHrn(organizationId, subOrganizationName ?: "", IamResources.USER, userId),
                request.policies.map { hrnFactory.getHrn(it) },
            )
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }
}

fun Route.resetPasswordApi() {
    val gson: Gson by inject()
    val usersService: UsersService by inject()

    post(
        "/organizations/{organization_id}/users/resetPassword",
        "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users/resetPassword",
    ) {
        val organizationId = call.parameters["organization_id"]
        val subOrganizationName = call.parameters["sub_organization_name"]
        call.principal<ApiPrincipal>()!!.tokenCredential.value!!
        val request = call.receive<ResetPasswordRequest>().validate()
        val user = usersService.getUserByEmail(organizationId!!, subOrganizationName, request.email)
        val response = usersService.setUserPassword(organizationId, subOrganizationName, user, request.password)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }
}

fun Route.createUserPasswordApi() {
    val gson: Gson by inject()
    val usersService: UsersService by inject()
    val passcodeRepo: PasscodeRepo by inject()
    val tokenService: TokenService by inject()

    postWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organization_id}/users/{user_id}/create_password",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
            ),
            RouteOption(
                "/organizations/{organization_id}/sub_organizations/{sub_organization_name}/users" +
                    "/{user_id}/create_password",
                resourceNameIndex = 4,
                resourceInstanceIndex = 5,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3,
            ),
        ),
        "createPassword",
    ) {
        val organizationId = call.parameters["organization_id"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val userId = call.parameters["user_id"]!!
        val principal = call.principal<UserPrincipal>()
        require(principal?.hrn?.organization == organizationId) {
            "Organization id in path and token are not matching. Invalid token"
        }
        val request = call.receive<CreateUserPasswordRequest>().validate()
        val inviteeHrn = ResourceHrn(organizationId, subOrganizationName ?: "", IamResources.USER, userId)
        val inviteeUser = usersService.getUser(inviteeHrn)

        if (principal?.tokenCredential?.type == TokenType.PASSCODE) {
            val passcode =
                passcodeRepo.getValidPasscodeById(
                    principal.tokenCredential.value!!,
                    VerifyEmailRequest.Purpose.invite,
                    organizationId = organizationId,
                ) ?: throw AuthenticationException("Invalid passcode")
            require(passcode.email == inviteeUser.email) { "Email in passcode does not match email in request" }
        }

        val user =
            usersService.createUserPassword(
                organizationId,
                subOrganizationName,
                userId,
                request.password,
            )

        val tokenResponse = tokenService.generateJwtToken(ResourceHrn(user.hrn))
        call.respondText(
            text = gson.toJson(tokenResponse),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created,
        )
    }
}

fun Route.linkUserApi() {
    val gson: Gson by inject()
    val usersService: UsersService by inject()

    postWithPermission(
        listOf(
            RouteOption(
                "/user_links",
                resourceNameIndex = 0,
                resourceInstanceIndex = 0,
            ),
        ),
        action = "createUserLink",
        validateOrgIdFromPath = false,
    ) {
        val principal = call.principal<UserPrincipal>()!!
        val request = call.receive<LinkUserRequest>().validate()
        val tokenResponse = usersService.linkUser(principal, request)
        call.respondText(
            text = gson.toJson(tokenResponse),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Created,
        )
    }

    getWithPermission(
        listOf(
            RouteOption(
                "/user_links",
                resourceNameIndex = 0,
                resourceInstanceIndex = 0,
            ),
        ),
        action = "listUserLinks",
        validateOrgIdFromPath = false,
    ) {
        val principal = call.principal<UserPrincipal>()!!
        val nextToken = call.request.queryParameters["next_token"]
        val pageSize = call.request.queryParameters["page_size"]
        val sortOrder = call.request.queryParameters["sort_order"]
        val role = call.request.queryParameters["role"]
        require(role != null && (role == "LEADER" || role == "SUBORDINATE")) {
            "Invalid value found for role"
        }
        val context =
            PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) },
                mapOf("role" to role),
            )
        val response = usersService.listUserLinks(principal, context)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    postWithPermission(
        listOf(
            RouteOption(
                "/user_links/{user_link_id}",
                resourceNameIndex = 0,
                resourceInstanceIndex = 1,
            ),
        ),
        action = "switchUser",
        validateOrgIdFromPath = false,
    ) {
        val linkId = call.parameters["user_link_id"]!!
        val principal = call.principal<UserPrincipal>()!!
        val response = usersService.switchUser(principal, linkId)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }

    deleteWithPermission(
        listOf(
            RouteOption(
                "/user_links/{user_link_id}",
                resourceNameIndex = 0,
                resourceInstanceIndex = 1,
            ),
        ),
        action = "deleteUserLink",
        validateOrgIdFromPath = false,
    ) {
        val linkId = call.parameters["user_link_id"]!!
        val principal = call.principal<UserPrincipal>()!!
        val response = usersService.deleteUserLink(principal, linkId)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }
}

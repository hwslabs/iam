package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.RouteOption
import com.hypto.iam.server.extensions.getWithPermission
import com.hypto.iam.server.extensions.post
import com.hypto.iam.server.extensions.postWithPermission
import com.hypto.iam.server.models.PaginationOptions
import com.hypto.iam.server.models.ResendInviteRequest
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.service.UsersService
import com.hypto.iam.server.utils.HrnFactory
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

@Suppress("ThrowsCount")
fun Route.createPasscodeApi() {
    val passcodeService: PasscodeService by inject()
    val usersService: UsersService by inject()
    val gson: Gson by inject()

    post("/verifyEmail") {
        val principal = context.principal<UserPrincipal>()
        val request = call.receive<VerifyEmailRequest>().validate()
        // TODO: createuser permission checks needed for this endpoint if purpose is invite
        // TODO: add tests for invite user purpose
        val email = if (request.email == null) {
            request.userHrn?.let {
                requireNotNull(principal) { "User is not authenticated. So can't send invite to ${request.userHrn}" }
                val inviteeHrn = kotlin
                    .runCatching { HrnFactory.getHrn(request.userHrn) as ResourceHrn }
                    .getOrNull() ?: throw BadRequestException("Invalid user hrn")
                val inviterHrn = HrnFactory.getHrn(principal.hrnStr) as ResourceHrn
                require(inviterHrn.organization == inviteeHrn.organization) {
                    "Authorization token doesn't belong to ${request.userHrn} organization"
                }
                // Adding a restriction to not allow sub organization users to send invites as we are not checking
                // permissions for sending invites today.
                // TODO: Add permission checks for sending invites
                require(inviterHrn.subOrganization.isNullOrEmpty()) {
                    "Sub organization users can't send invites"
                }
                val inviteeUser = usersService.getUser(inviteeHrn)
                inviteeUser.email ?: throw BadRequestException("UserHrn ${request.userHrn} doesn't have email")
            } ?: throw BadRequestException("Email or userHrn is required")
        } else {
            request.email.lowercase()
        }

        // Validations
        principal?.let {
            if (request.organizationId != null) {
                require(it.organization == request.organizationId) {
                    "Does not have permission to call api for organization ${request.organizationId}"
                }
            }
        }
        if (request.purpose == VerifyEmailRequest.Purpose.invite) {
            requireNotNull(principal) { "User must be logged in for invite purpose" }
            val inviteMetadata = InviteMetadata(request.metadata!!)
            require(inviteMetadata.inviterUserHrn == principal.hrnStr) { "Does not support cross user invites" }
        }

        val response = passcodeService.verifyEmail(
            email,
            request.userHrn,
            request.purpose,
            request.organizationId,
            request.subOrganizationName,
            request.metadata,
            principal
        )
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

fun Route.passcodeApis() {
    val passcodeService: PasscodeService by inject()
    val gson: Gson by inject()

    getWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organizationId}/invites",
                resourceNameIndex = 0,
                resourceInstanceIndex = 1,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organizationId}/sub_organizations/{sub_organization_name}/invites",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3
            )
        ),
        "listInvites",
    ) {
        val organizationId = call.parameters["organizationId"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]

        val nextToken = call.request.queryParameters["next_token"]
        val pageSize = call.request.queryParameters["page_size"]
        val sortOrder = call.request.queryParameters["sort_order"]

        val context = PaginationContext.from(
            nextToken,
            pageSize?.toInt(),
            sortOrder?.let { PaginationOptions.SortOrder.valueOf(it) }
        )

        val passcodes = passcodeService.listOrgPasscodes(
            organizationId,
            subOrganizationName,
            VerifyEmailRequest.Purpose.invite,
            context
        )
        call.respondText(
            text = gson.toJson(passcodes),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }

    postWithPermission(
        listOf(
            RouteOption(
                "/organizations/{organizationId}/invites/resend",
                resourceNameIndex = 0,
                resourceInstanceIndex = 1,
                organizationIdIndex = 1
            ),
            RouteOption(
                "/organizations/{organizationId}/sub_organizations/{sub_organization_name}/invites/resend",
                resourceNameIndex = 2,
                resourceInstanceIndex = 3,
                organizationIdIndex = 1,
                subOrganizationNameIndex = 3
            )
        ),
        "resendInvite",
    ) {
        val principal = context.principal<UserPrincipal>()!!
        val organizationId = call.parameters["organizationId"]!!
        val subOrganizationName = call.parameters["sub_organization_name"]
        val request = call.receive<ResendInviteRequest>().validate()

        val passcode = passcodeService.resendInvitePasscode(
            organizationId,
            subOrganizationName,
            request.email,
            principal
        )
        call.respondText(
            text = gson.toJson(passcode),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

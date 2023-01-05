package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.validators.InviteMetadata
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.passcodeApi() {
    val passcodeService: PasscodeService by inject()
    val gson: Gson by inject()

    post("/verifyEmail") {
        val principal = context.principal<UserPrincipal>()
        val request = call.receive<VerifyEmailRequest>().validate()
        // TODO: createuser permission checks needed for this endpoint if purpose is invite

        // Validations
        principal?.let {
            if (request.organizationId != null) {
                require(it.organization == request.organizationId) {
                    "Does not have permission to call api for organization ${request.organizationId}"
                }
            }
        }
        if (request.purpose == VerifyEmailRequest.Purpose.invite) {
            val inviteMetadata = InviteMetadata(request.metadata!!)
            require(inviteMetadata.inviterUserHrn == principal!!.hrnStr) {
                "Does not support cross user invites"
            }
        }

        val response = passcodeService.verifyEmail(
            request.email,
            request.purpose,
            request.organizationId,
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

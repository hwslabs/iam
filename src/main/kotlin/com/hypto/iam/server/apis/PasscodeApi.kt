package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.VerifyEmailRequest
import com.hypto.iam.server.plugins.inject
import com.hypto.iam.server.service.PasscodeService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.passcodeApi() {
    val passcodeService: PasscodeService by inject()
    val gson: Gson by inject()

    post("/verifyEmail") {
        val request = call.receive<VerifyEmailRequest>().validate()
        val response = passcodeService.verifyEmail(request.email)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

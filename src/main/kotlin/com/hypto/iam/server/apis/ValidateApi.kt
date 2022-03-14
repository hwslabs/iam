package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.ValidationRequest
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.ValidationService
import com.hypto.iam.server.validators.validate
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import org.koin.ktor.ext.inject

fun Route.validationApi() {
    val validationService: ValidationService by inject()
    val gson: Gson by inject()

    post("/validate") {
        val principal = context.principal<UserPrincipal>()!!

        val request = call.receive<ValidationRequest>().validate()

        val response = validationService.validateIfUserHasPermissionToActions(principal.hrn, request)

        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.TokenService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.accept
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

val tokenService: TokenService = getKoinInstance()
val gson: Gson = getKoinInstance()

suspend fun generateToken(call: ApplicationCall, context: ApplicationCall) {
    val principal = context.principal<UserPrincipal>()!!
    val responseContentType = context.request.accept()
    val response =
        if (principal.tokenCredential.type == TokenType.JWT && principal.tokenCredential.value != null) {
            TokenResponse(token = principal.tokenCredential.value)
        } else {
            tokenService.generateJwtToken(principal.hrn)
        }

    when (responseContentType) {
        ContentType.Text.Plain.toString() -> call.respondText(
            text = response.token,
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.OK
        )
        else -> call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}
fun Route.tokenApi() {
    authenticate("basic-auth", "bearer-auth") {
        post("/organizations/{organization_id}/token") {
            generateToken(call, context)
        }
    }

    authenticate("basic-auth", "unique-basic-auth", "bearer-auth") {
        post("/authenticate") {
            generateToken(call, context)
        }
    }
}

fun Route.loginApi() {
    authenticate("unique-basic-auth", "bearer-auth") {
        post("/login") {
            generateToken(call, context)
        }
    }
}

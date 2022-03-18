package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.TokenService
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import org.koin.ktor.ext.inject

fun Route.tokenApi() {
    val tokenService: TokenService by inject()
    val gson: Gson by inject()

    post("/token") {
        val principal = context.principal<UserPrincipal>()!!
        // TODO: [IMPORTANT] Check if the user has permission to invoke this API and return / raise if necessary
        val response = tokenService.generateJwtToken(principal.hrn)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

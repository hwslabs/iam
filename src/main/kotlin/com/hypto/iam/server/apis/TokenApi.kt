package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.plugins.inject
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.TokenService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.tokenApi() {
    val tokenService: TokenService by inject()
    val gson: Gson by inject()

    post("/organizations/{organization_id}/token") {
        val principal = context.principal<UserPrincipal>()!!
        val response = tokenService.generateJwtToken(principal.hrn)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    }
}

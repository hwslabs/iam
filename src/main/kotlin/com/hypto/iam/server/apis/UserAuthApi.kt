package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.models.AddUserAuthMethodRequest
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.UserAuthService
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.userAuthApi() {
    val userAuthService: UserAuthService by inject()
    val gson: Gson by inject()

    withPermission(
        "getUserAuth",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1),
    ) {
        get("/organizations/{organization_id}/users/{id}/auth_methods") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["id"]!!
            val response = userAuthService.listUserAuth(organizationId, userId)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }

    withPermission(
        "createUserAuth",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1),
    ) {
        post("/organizations/{organization_id}/users/{id}/auth_methods") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["id"]!!
            val issuer = call.request.headers["x-issuer"] ?: throw BadRequestException("x-issuer header is missing")
            val request = call.receive<AddUserAuthMethodRequest>().validate()
            val token = request.token ?: throw BadRequestException("token is missing")
            val principal = context.principal<UserPrincipal>()!!
            val response = userAuthService.createUserAuth(organizationId, userId, issuer, token, principal)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Created,
            )
        }
    }
}

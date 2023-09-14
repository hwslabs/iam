package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import com.hypto.iam.server.service.UserAuthService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.userAuthApi() {
    val userAuthService: UserAuthService by inject()
    val gson: Gson by inject()

    withPermission(
        "getUserAuth",
        getResourceHrnFunc(resourceNameIndex = 2, resourceInstanceIndex = 3, organizationIdIndex = 1)
    ) {
        get("/organizations/{organization_id}/users/{id}/auth_methods") {
            val organizationId = call.parameters["organization_id"]!!
            val userId = call.parameters["id"]!!
            val response = userAuthService.listUserAuth(organizationId, userId)
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}

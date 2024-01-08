package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.service.AuthProviderService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.authProviderApi() {
    val authProviderService: AuthProviderService by inject()
    val gson: Gson by inject()

    get("/auth_providers") {
        val nextToken = call.request.queryParameters["next_token"]
        val pageSize = call.request.queryParameters["page_size"]

        val paginationContext =
            PaginationContext.from(
                nextToken,
                pageSize?.toInt(),
                null,
            )

        val response = authProviderService.listAuthProvider(paginationContext)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }
}

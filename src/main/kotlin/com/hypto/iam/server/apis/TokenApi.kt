package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.models.GetDelegateTokenRequest
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.security.ApiPrincipal
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.service.TokenService
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.validators.validate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.accept
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import okhttp3.OkHttpClient
import okhttp3.Request

private val tokenService: TokenService = getKoinInstance()
private val gson: Gson = getKoinInstance()

@Suppress("ThrowsCount")
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

@Suppress("ThrowsCount")
suspend fun generateTokenOauth(call: ApplicationCall, context: ApplicationCall) {
    val principal = context.principal<ApiPrincipal>()!!
    val responseContentType = context.request.accept()
    val token = principal.tokenCredential?.value!!
    val response = when (call.request.headers["issuer"]!!) {
        "google" -> {
            val httpClient = OkHttpClient()
            val requestBuilder = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/userinfo?access_token=$token")
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw BadRequestException("Invalid token")
            }
            val responseBody = response.body?.let { String(it.bytes()) }
            val googleUser = gson.fromJson(responseBody, GoogleUser::class.java)
            val user = UserRepo.findByEmail(googleUser.email) ?: throw BadRequestException("User not signed up")
            tokenService.generateJwtToken(ResourceHrn(user.hrn))
        }
        else -> {
            throw BadRequestException("Invalid issuer")
        }
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

    authenticate("unique-basic-auth", "bearer-auth") {
        post("/token") {
            generateToken(call, context)
        }
    }

    authenticate("basic-auth", "bearer-auth") {
        post("delegate_token") {
            // No static validations required for GetDelegateTokenRequest
            val request = call.receive<GetDelegateTokenRequest>().validate()
            val responseContentType = context.request.accept()
            val principal = context.principal<UserPrincipal>()!!
            val response = tokenService.generateDelegateJwtToken(principal, request)

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
    }

    authenticate("basic-auth", "unique-basic-auth", "bearer-auth") {
        post("/authenticate") {
            generateToken(call, context)
        }
    }
}

fun Route.loginApi() {
    authenticate("unique-basic-auth", "bearer-auth", "oauth") {
        post("/login") {
            val principal = context.principal<UserPrincipal>()
            if (principal == null) {
                generateTokenOauth(call, context)
            } else {
                generateToken(call, context)
            }
        }
    }
}

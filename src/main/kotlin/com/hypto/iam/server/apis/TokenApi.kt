package com.hypto.iam.server.apis

import com.google.gson.Gson
import com.hypto.iam.server.authProviders.AuthProviderRegistry
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.extensions.post
import com.hypto.iam.server.models.GetDelegateTokenRequest
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.OAuthUserPrincipal
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
import io.ktor.server.request.accept
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route

private val tokenService: TokenService = getKoinInstance()
private val gson: Gson = getKoinInstance()
private val userRepo = getKoinInstance<UserRepo>()
private val userAuthRepo = getKoinInstance<UserAuthRepo>()

@Suppress("ThrowsCount")
suspend fun generateToken(
    call: ApplicationCall,
    context: ApplicationCall,
) {
    val principal = context.principal<UserPrincipal>()!!
    val responseContentType = context.request.accept()
    val response =
        if (principal.tokenCredential.type == TokenType.JWT && principal.tokenCredential.value != null) {
            TokenResponse(token = principal.tokenCredential.value)
        } else {
            tokenService.generateJwtToken(principal.hrn)
        }

    when (responseContentType) {
        ContentType.Text.Plain.toString() ->
            call.respondText(
                text = response.token,
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.OK,
            )
        else ->
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
    }
}

@Suppress("ThrowsCount")
suspend fun generateTokenOauth(
    call: ApplicationCall,
    context: ApplicationCall,
) {
    val principal = context.principal<OAuthUserPrincipal>()!!
    val responseContentType = context.request.accept()
    val user = userRepo.findByEmail(principal.email) ?: throw AuthenticationException("User has not signed up yet")
    var userAuth = userAuthRepo.fetchByUserHrnAndProviderName(user.hrn, principal.issuer)

    val authProvider =
        AuthProviderRegistry.getProvider(principal.issuer) ?: throw AuthenticationException(
            "Invalid issuer",
        )
    if (authProvider.isVerifiedProvider && userAuth == null) {
        userAuth =
            userAuthRepo.create(
                user.hrn,
                principal.issuer,
                principal.metadata?.let { AuthMetadata.toJsonB(it) },
            )
    }
    userAuth?.let { authProvider.authenticate(principal, it) }
        ?: throw AuthenticationException("User has not signed up yet")

    val response = tokenService.generateJwtToken(ResourceHrn(user.hrn))

    when (responseContentType) {
        ContentType.Text.Plain.toString() ->
            call.respondText(
                text = response.token,
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.OK,
            )
        else ->
            call.respondText(
                text = gson.toJson(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
    }
}

fun Route.tokenApi() {
    authenticate("basic-auth", "bearer-auth") {
        post("/organizations/{organization_id}/token") {
            generateToken(call, context)
        }
    }

    authenticate("sub-org-basic-auth", "bearer-auth") {
        post("/organizations/{organization_id}/sub_organizations/token") {
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
                ContentType.Text.Plain.toString() ->
                    call.respondText(
                        text = response.token,
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.OK,
                    )
                else ->
                    call.respondText(
                        text = gson.toJson(response),
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
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
            context.principal<OAuthUserPrincipal>()?.let {
                generateTokenOauth(call, context)
            } ?: generateToken(call, context)
        }
    }
}

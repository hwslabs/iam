package com.hypto.iam.server.infrastructure

import com.hypto.iam.server.models.Policy
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.auth.*
import io.ktor.request.ApplicationRequest
import io.ktor.response.respond

enum class TokenLocation(val location: String) {
    QUERY("query"),
    HEADER("header")
}

/** Class which stores the token credentials sent by the client */
data class TokenCredential(val value: String?) : Credential

/** Class to store the Principal authenticated using ApiKey auth **/
data class ApiPrincipal(
    val tokenCredential: TokenCredential,
    val organization: String
) : Principal

/** Class to store the Principal authenticated using Bearer auth **/
data class UserPrincipal(
    val tokenCredential: TokenCredential,
    val organization: String,
    val userId: String,
    val policies: List<Policy>? = null
) : Principal

/**
 * Represents a Api Key authentication provider
 * @param name is the name of the provider, or `null` for a default provider
 */
class TokenAuthenticationProvider(config: Configuration) : AuthenticationProvider(config) {
    internal var authenticationFunction: suspend ApplicationCall.(TokenCredential) -> Principal? = { null }

    var tokenKeyName: String = "X-Api-Key" // Default api key

    var tokenKeyLocation: TokenLocation = TokenLocation.HEADER

    /**
     * Sets a validation function that will check given [ApiKeyCredential] instance and return [Principal],
     * or null if credential does not correspond to an authenticated principal
     */
    fun validate(body: suspend ApplicationCall.(TokenCredential) -> Principal?) {
        authenticationFunction = body
    }
}

class ApiKeyConfiguration(name: String?) : AuthenticationProvider.Configuration(name) {
    // todo
}

fun Authentication.Configuration.apiKeyAuth(name: String? = null, configure: TokenAuthenticationProvider.() -> Unit) {
    val provider = TokenAuthenticationProvider(ApiKeyConfiguration(name)).apply(configure)
    val apiKeyName = provider.tokenKeyName
    val apiKeyLocation = provider.tokenKeyLocation
    val authenticate = provider.authenticationFunction

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val credentials = call.request.tokenAuthenticationCredentials(apiKeyName, apiKeyLocation)
        val principal = credentials?.let { authenticate(call, it) }
        validateResponse(credentials, principal, context, apiKeyName)
    }
    register(provider)
}

fun Authentication.Configuration.bearer(name: String? = null, configure: TokenAuthenticationProvider.() -> Unit) {
    val provider = TokenAuthenticationProvider(ApiKeyConfiguration(name))
        .apply(configure) // Apply the validation configuration given by the caller

    val apiKeyName = "Authorization"
    val apiKeyLocation = TokenLocation.HEADER
    val authenticate = provider.authenticationFunction

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val credentials = call.request.tokenAuthenticationCredentials(apiKeyName, apiKeyLocation) {
            when (val header = parseAuthorizationHeader(it)) {
                is HttpAuthHeader.Single -> header.blob
                else -> null
            }
        }
        val principal = credentials?.let { authenticate(call, it) }

        validateResponse(credentials, principal, context, apiKeyName)
    }
    register(provider)
}

private fun validateResponse(
    credentials: TokenCredential?,
    principal: Principal?,
    context: AuthenticationContext,
    apiKeyName: String
) {
    val cause = when {
        credentials == null -> AuthenticationFailedCause.NoCredentials
        principal == null -> AuthenticationFailedCause.InvalidCredentials
        else -> null
    }

    if (cause != null) {
        context.challenge(apiKeyName, cause) {
            call.respond(
                UnauthorizedResponse(
                    HttpAuthHeader.Parameterized(
                        "API_KEY",
                        mapOf("key" to apiKeyName),
                        HeaderValueEncoding.QUOTED_ALWAYS
                    )
                )
            )
            it.complete()
        }
    }

    if (principal != null) {
        context.principal(principal)
    }
}

fun ApplicationRequest.tokenAuthenticationCredentials(
    apiKeyName: String,
    tokenLocation: TokenLocation,
    transform: ((String) -> String?)? = null
): TokenCredential? {
    val value: String? = when (tokenLocation) {
        TokenLocation.QUERY -> this.queryParameters[apiKeyName]
        TokenLocation.HEADER -> this.headers[apiKeyName]
    }
    val result = if (transform != null && value != null) {
        transform.invoke(value)
    } else value
    return when (result) {
        null -> null
        else -> TokenCredential(result)
    }
}

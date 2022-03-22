package com.hypto.iam.server.security

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.extensions.MagicNumber
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.policy.PolicyBuilder
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationContext
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.Credential
import io.ktor.auth.Principal
import io.ktor.auth.UnauthorizedResponse
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.request.ApplicationRequest
import io.ktor.response.respond
import java.util.Base64

data class AuthenticationException(override val message: String) : Exception(message)

enum class TokenLocation(val location: String) {
    QUERY("query"),
    HEADER("header")
}

enum class TokenType(val type: String) {
    CREDENTIAL("credential"),
    JWT("jwt")
}

/** Class which stores the token credentials sent by the client */
data class TokenCredential(val value: String?, val type: TokenType?) : Credential

/** Class to store the Principal authenticated using ApiKey auth **/
data class ApiPrincipal(
    val tokenCredential: TokenCredential,
    val organization: String
) : Principal

/** Class to store the Principal authenticated using Bearer auth **/
data class UserPrincipal(
    val tokenCredential: TokenCredential,
    val hrnStr: String,
    val policies: PolicyBuilder
) : Principal {
    val hrn: Hrn = HrnFactory.getHrn(hrnStr)
}

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

/**
 * Represents an Api Key authentication provider
 * @param name is the name of the provider, or `null` for a default provider
 */
class ApiKeyConfiguration(name: String?) : AuthenticationProvider.Configuration(name)

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
            val result = when (val header = parseAuthorizationHeader(it)) {
                is HttpAuthHeader.Single -> header.blob
                else -> null
            }
            return@tokenAuthenticationCredentials result
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
            call.respond(UnauthorizedResponse())
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
    val type = if (result != null && isIamJWT(result)) { TokenType.JWT } else { TokenType.CREDENTIAL }
    return when (result) {
        null -> null
        else -> TokenCredential(result, type)
    }
}

private const val ALGORITHM = "alg"
private const val KEY_ID = "kid"

private val gson: Gson = getKoinInstance()

fun isIamJWT(jwt: String): Boolean {
    val jwtComponents = jwt.split(".")
    if (jwtComponents.size != MagicNumber.THREE) // The JWT is composed of three parts
        return false
    var result = true
    try {
        val jsonFirstPart = String(Base64.getDecoder().decode(jwtComponents[0]))
        // The first part of the JWT is a JSON
        val firstPart = gson.fromJson(jsonFirstPart, JsonElement::class.java).asJsonObject
        // The first part has the attribute "alg" and "kid"
        if (!firstPart.has(ALGORITHM) || !firstPart.has(KEY_ID))
            result = false
        // Put the validations you think are necessary for the data the JWT should take to validate
    } catch (err: JsonParseException) {
        result = false
    }
    return result
}

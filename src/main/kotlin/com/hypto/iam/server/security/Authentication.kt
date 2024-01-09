package com.hypto.iam.server.security

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.hypto.iam.server.Constants
import com.hypto.iam.server.Constants.Companion.AUTHORIZATION_HEADER
import com.hypto.iam.server.Constants.Companion.X_API_KEY_HEADER
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.extensions.MagicNumber
import com.hypto.iam.server.service.TokenServiceImpl
import com.hypto.iam.server.service.UserPrincipalService
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.policy.PolicyBuilder
import io.jsonwebtoken.Claims
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.Credential
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import mu.KotlinLogging
import java.util.Base64

private val logger = KotlinLogging.logger { }

data class AuthenticationException(override val message: String) : Exception(message)

enum class TokenLocation(val location: String) {
    QUERY("query"),
    HEADER("header"),
}

enum class TokenType(val type: String) {
    CREDENTIAL("credential"),
    JWT("jwt"),
    BASIC("basic"),
    PASSCODE("passcode"),
    OAUTH("oauth"),
}

/** Class which stores the token credentials sent by the client */
data class TokenCredential(val value: String?, val type: TokenType?) : Credential

interface IamPrincipal : Principal {
    val tokenCredential: TokenCredential
    val organization: String
    val issuer: String
        get() = TokenServiceImpl.ISSUER
}

/** Class which stores email and password authenticated using Unique Basic Auth */
data class UsernamePasswordCredential(val username: String, val password: String)

/** Class to store the Principal authenticated using ApiKey auth **/
data class ApiPrincipal(
    override val tokenCredential: TokenCredential,
    override val organization: String,
    val policies: PolicyBuilder? = null,
) : IamPrincipal

/** Class to store the Principal authenticated using Bearer auth **/
data class UserPrincipal(
    override val tokenCredential: TokenCredential,
    val hrnStr: String,
    val claims: Claims? = null,
    val policies: PolicyBuilder,
) : IamPrincipal {
    val hrn: Hrn = HrnFactory.getHrn(hrnStr)
    override val organization: String = hrn.organization
}

/** Class to store the Principal authenticated using Oauth auth **/
data class OAuthUserPrincipal(
    override val tokenCredential: TokenCredential,
    override val organization: String,
    val email: String,
    val name: String,
    val companyName: String,
    override val issuer: String,
) : IamPrincipal

class TokenAuthenticationProvider internal constructor(
    config: Config,
) : AuthenticationProvider(config) {
    internal var authenticationFunction = config.authenticationFunction
    private val tokenKeyName = config.keyName
    private val tokenKeyLocation = config.keyLocation
    private val tokenType = config.tokenType
    private val authSchemeExists = config.authSchemeExists
    private val optionalAuth = config.optionalAuth

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val credentials =
            call.request.tokenAuthenticationCredentials(tokenKeyName, tokenKeyLocation, tokenType) {
                if (!authSchemeExists) {
                    return@tokenAuthenticationCredentials it
                }
                if (call.request.headers.contains("x-issuer")) {
                    if (!it.startsWith("Bearer")) {
                        throw AuthenticationException("Invalid token")
                    } else {
                        return@tokenAuthenticationCredentials it.substringAfter("Bearer ")
                    }
                }
                @Suppress("TooGenericExceptionCaught")
                try {
                    val result =
                        when (val header = parseAuthorizationHeader(it)) {
                            is HttpAuthHeader.Single -> header.blob
                            else -> null
                        }
                    return@tokenAuthenticationCredentials result
                } catch (e: Exception) {
                    logger.error(e) { "Invalid token" }
                    throw AuthenticationException("Invalid token")
                }
            }
        val principal = credentials?.let { authenticationFunction(call, it) }

        validateResponse(credentials, principal, context, tokenKeyName, optionalAuth)
    }

    /**
     * A configuration for the [TokenAuthenticationProvider].
     */
    open class Config internal constructor(
        name: String?,
        val keyName: String,
        val keyLocation: TokenLocation = TokenLocation.HEADER,
        val authSchemeExists: Boolean = false,
        val tokenType: TokenType? = null,
        val optionalAuth: Boolean = false,
    ) : AuthenticationProvider.Config(name) {
        internal var authenticationFunction: suspend ApplicationCall.(TokenCredential) -> IamPrincipal? = {
            throw NotImplementedError("Token auth validate function is not specified.")
        }

        /**
         * Sets a validation function that checks a specified [TokenCredential] instance and
         * returns [Principal] in a case of successful authentication or null if authentication fails.
         */
        fun validate(body: suspend ApplicationCall.(TokenCredential) -> IamPrincipal?) {
            authenticationFunction = body
        }
    }
}

fun AuthenticationConfig.apiKeyAuth(
    name: String? = null,
    configure: TokenAuthenticationProvider.Config.() -> Unit,
) {
    val provider =
        TokenAuthenticationProvider(
            TokenAuthenticationProvider.Config(name, X_API_KEY_HEADER).apply(configure),
        )
    register(provider)
}

fun AuthenticationConfig.passcodeAuth(
    name: String? = null,
    configure: TokenAuthenticationProvider.Config.() -> Unit,
) {
    val provider =
        TokenAuthenticationProvider(
            TokenAuthenticationProvider.Config(name, X_API_KEY_HEADER, tokenType = TokenType.PASSCODE).apply(configure),
        )
    register(provider)
}

fun AuthenticationConfig.bearer(
    name: String? = null,
    configure: TokenAuthenticationProvider.Config.() -> Unit,
) {
    val provider =
        TokenAuthenticationProvider(
            TokenAuthenticationProvider.Config(name, AUTHORIZATION_HEADER, authSchemeExists = true).apply(configure),
        )
    register(provider)
}

fun AuthenticationConfig.oauth(
    name: String? = null,
    configure: TokenAuthenticationProvider.Config.() -> Unit,
) {
    val provider =
        TokenAuthenticationProvider(
            TokenAuthenticationProvider.Config(
                name,
                AUTHORIZATION_HEADER,
                authSchemeExists = true,
                tokenType = TokenType.OAUTH,
            ).apply(configure),
        )
    register(provider)
}

fun AuthenticationConfig.optionalBearer(
    name: String? = null,
    configure: TokenAuthenticationProvider.Config.() -> Unit,
) {
    val provider =
        TokenAuthenticationProvider(
            TokenAuthenticationProvider.Config(
                name,
                AUTHORIZATION_HEADER,
                authSchemeExists = true,
                optionalAuth = true,
            ).apply(configure),
        )
    register(provider)
}

private fun validateResponse(
    credentials: TokenCredential?,
    principal: IamPrincipal?,
    context: AuthenticationContext,
    apiKeyName: String,
    optionalAuth: Boolean,
) {
    val cause =
        when {
            credentials == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

    if (cause != null && !optionalAuth) {
        context.challenge(apiKeyName, cause) { challenge, call ->
            call.respond(UnauthorizedResponse())
            challenge.complete()
        }
    }

    if (principal != null) {
        context.principal(principal)
        context.call.response.headers.append(Constants.X_ORGANIZATION_HEADER, principal.organization)
    }
}

fun ApplicationRequest.tokenAuthenticationCredentials(
    apiKeyName: String,
    tokenLocation: TokenLocation,
    tokenType: TokenType? = null,
    transform: ((String) -> String?)? = null,
): TokenCredential? {
    val value: String? =
        when (tokenLocation) {
            TokenLocation.QUERY -> this.queryParameters[apiKeyName]
            TokenLocation.HEADER -> this.headers[apiKeyName]
        }
    val result =
        if (transform != null && value != null) {
            transform.invoke(value)
        } else {
            value
        }
    val type =
        tokenType ?: if (result != null && isIamJWT(result)) {
            TokenType.JWT
        } else {
            TokenType.CREDENTIAL
        }

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
    if (jwtComponents.size != MagicNumber.THREE) {
        // The JWT is composed of three parts
        return false
    }
    var result = true
    try {
        val jsonFirstPart = String(Base64.getDecoder().decode(jwtComponents[0]))
        // The first part of the JWT is a JSON
        val firstPart = gson.fromJson(jsonFirstPart, JsonElement::class.java).asJsonObject
        // The first part has the attribute "alg" and "kid"
        if (!firstPart.has(ALGORITHM) || !firstPart.has(KEY_ID)) {
            result = false
        }
        // Put the validations you think are necessary for the data the JWT should take to validate
    } catch (err: JsonParseException) {
        result = false
    }
    return result
}

fun bearerAuthValidation(
    userPrincipalService: UserPrincipalService,
): suspend ApplicationCall.(tokenCredential: TokenCredential) -> UserPrincipal? {
    return { tokenCredential ->
        if (tokenCredential.value == null) {
            null
        }
        try {
            when (tokenCredential.type) {
                TokenType.CREDENTIAL ->
                    userPrincipalService.getUserPrincipalByRefreshToken(
                        tokenCredential,
                    )
                TokenType.JWT -> userPrincipalService.getUserPrincipalByJwtToken(tokenCredential)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

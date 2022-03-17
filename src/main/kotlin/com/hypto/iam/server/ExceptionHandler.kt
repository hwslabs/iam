package com.hypto.iam.server

import com.hypto.iam.server.db.listeners.DeleteOrUpdateWithoutWhereException
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.extensions.PaginationContext.Companion.gson
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.AuthorizationException
import com.hypto.iam.server.utils.HrnParseException
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import org.jooq.exception.DataAccessException

inline fun <reified T : Throwable> StatusPages.Configuration.sendStatus(
    statusCode: HttpStatusCode,
    shouldThrowException: Boolean = false,
    message: String? = null
) {
    exception<T> { cause ->
        val response = ErrorResponse(cause.message ?: message)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = statusCode
        )
        shouldThrowException && throw cause
    }
}

data class ErrorResponse(val message: String?)

fun StatusPages.Configuration.statusPages() {
    sendStatus<AuthenticationException>(HttpStatusCode.Unauthorized)
    sendStatus<AuthorizationException>(HttpStatusCode.Forbidden)
    sendStatus<EntityAlreadyExistsException>(HttpStatusCode.BadRequest)
    sendStatus<EntityNotFoundException>(HttpStatusCode.NotFound)
    sendStatus<InternalException>(HttpStatusCode.InternalServerError, true)
    sendStatus<DeleteOrUpdateWithoutWhereException>(HttpStatusCode.InternalServerError)
    sendStatus<HrnParseException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalAccessException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalStateException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalArgumentException>(HttpStatusCode.BadRequest)
    sendStatus<DataAccessException>(HttpStatusCode.Unauthorized)
    sendStatus<UnknownError>(HttpStatusCode.InternalServerError, true, "Unknown Error Occurred")
    sendStatus<Throwable>(HttpStatusCode.InternalServerError, true, "Internal Server Error Occurred")
    /* TODO: Handle the following exceptions

        UnsupportedJwtException – if the claimsJws argument does not represent an Claims JWS
        MalformedJwtException – if the claimsJws string is not a valid JWS
        SignatureException – if the claimsJws JWS signature validation fails
        JwtExpiredException – if the specified JWT is a Claims JWT and the Claims has
                an expiration time before the time this method is invoked.
     */
}

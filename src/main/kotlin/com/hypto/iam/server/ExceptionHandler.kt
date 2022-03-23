package com.hypto.iam.server

import com.hypto.iam.server.db.listeners.DeleteOrUpdateWithoutWhereException
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.exceptions.JwtExpiredException
import com.hypto.iam.server.exceptions.PolicyFormatException
import com.hypto.iam.server.extensions.PaginationContext.Companion.gson
import com.hypto.iam.server.idp.UserAlreadyExistException
import com.hypto.iam.server.idp.UserNotFoundException
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.AuthorizationException
import com.hypto.iam.server.service.OrganizationAlreadyExistException
import com.hypto.iam.server.utils.HrnParseException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import java.security.SignatureException
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
    sendStatus<UserAlreadyExistException>(HttpStatusCode.BadRequest)
    sendStatus<OrganizationAlreadyExistException>(HttpStatusCode.BadRequest)
    sendStatus<EntityNotFoundException>(HttpStatusCode.NotFound)
    sendStatus<UserNotFoundException>(HttpStatusCode.NotFound)
    sendStatus<UnsupportedJwtException>(HttpStatusCode.BadRequest)
    sendStatus<MalformedJwtException>(HttpStatusCode.BadRequest)
    sendStatus<SignatureException>(HttpStatusCode.BadRequest)
    sendStatus<JwtExpiredException>(HttpStatusCode.Unauthorized)
    sendStatus<PolicyFormatException>(HttpStatusCode.BadRequest)
    sendStatus<InternalException>(HttpStatusCode.InternalServerError, true)
    sendStatus<DeleteOrUpdateWithoutWhereException>(HttpStatusCode.InternalServerError)
    sendStatus<HrnParseException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalAccessException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalStateException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalArgumentException>(HttpStatusCode.BadRequest)
    sendStatus<DataAccessException>(HttpStatusCode.Unauthorized)
    sendStatus<UnknownError>(HttpStatusCode.InternalServerError, true, "Unknown Error Occurred")
    sendStatus<Throwable>(HttpStatusCode.InternalServerError, true, "Internal Server Error Occurred")
}

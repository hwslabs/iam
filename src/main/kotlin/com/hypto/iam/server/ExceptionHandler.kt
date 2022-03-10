package com.hypto.iam.server

import com.hypto.iam.server.db.listeners.DeleteOrUpdateWithoutWhereException
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.extensions.PaginationContext.Companion.gson
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.AuthorizationException
import com.hypto.iam.server.utils.HrnParseException
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import org.jooq.exception.DataAccessException

data class ErrorResponse(val message: String?)

inline fun <reified T : Throwable> StatusPages.Configuration.sendStatus(
    statusCode: HttpStatusCode,
    message: String? = null
) {
    exception<T> { cause ->
        val response = ErrorResponse(cause.message ?: message)
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = statusCode
        )
        throw cause
    }
}


fun StatusPages.Configuration.statusPages() {
    sendStatus<AuthenticationException>(HttpStatusCode.Unauthorized)
    sendStatus<AuthorizationException>(HttpStatusCode.Forbidden)
    sendStatus<EntityAlreadyExistsException>(HttpStatusCode.BadRequest)
    sendStatus<EntityNotFoundException>(HttpStatusCode.NotFound)
    sendStatus<InternalException>(HttpStatusCode.InternalServerError)
    sendStatus<DeleteOrUpdateWithoutWhereException>(HttpStatusCode.InternalServerError)
    sendStatus<HrnParseException>(HttpStatusCode.InternalServerError)
    sendStatus<IllegalAccessException>(HttpStatusCode.InternalServerError)
    sendStatus<IllegalStateException>(HttpStatusCode.InternalServerError)
    sendStatus<IllegalArgumentException>(HttpStatusCode.BadRequest)
    sendStatus<DataAccessException>(HttpStatusCode.Unauthorized)
    sendStatus<UnknownError>(HttpStatusCode.InternalServerError, "Unknown Error Occurred")
    sendStatus<Throwable>(HttpStatusCode.InternalServerError, "Internal Server Error Occurred")
}

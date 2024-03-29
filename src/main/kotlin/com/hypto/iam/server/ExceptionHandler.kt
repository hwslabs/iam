package com.hypto.iam.server

import com.hypto.iam.server.db.listeners.DeleteOrUpdateWithoutWhereException
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.exceptions.PasscodeExpiredException
import com.hypto.iam.server.exceptions.PasscodeLimitExceededException
import com.hypto.iam.server.exceptions.PolicyFormatException
import com.hypto.iam.server.exceptions.UnknownException
import com.hypto.iam.server.extensions.PaginationContext.Companion.gson
import com.hypto.iam.server.idp.UserAlreadyExistException
import com.hypto.iam.server.idp.UserNotFoundException
import com.hypto.iam.server.models.ErrorResponse
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.AuthorizationException
import com.hypto.iam.server.service.OrganizationAlreadyExistException
import com.hypto.iam.server.utils.HrnParseException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respondText
import mu.KotlinLogging
import org.jooq.exception.DataAccessException
import java.security.SignatureException

val logger = KotlinLogging.logger { }

inline fun <reified T : Throwable> StatusPagesConfig.sendStatus(
    statusCode: HttpStatusCode,
    shouldThrowException: Boolean = false,
    message: String? = null,
) {
    exception<T> { call, cause ->
        val response =
            ErrorResponse(
                message
                    ?: if (
                        statusCode.value < HttpStatusCode.InternalServerError.value &&
                        cause.message != null
                    ) {
                        cause.message!!
                    } else {
                        "Unknown Error Occurred"
                    },
            )
        if (statusCode.value == HttpStatusCode.InternalServerError.value) {
            logger.error(cause) { "**** Attention: Internal Server Error ****" }
        }
        call.respondText(
            text = gson.toJson(response),
            contentType = ContentType.Application.Json,
            status = statusCode,
        )
        shouldThrowException && throw cause
    }
}

fun StatusPagesConfig.statusPages() {
    sendStatus<AuthenticationException>(HttpStatusCode.Unauthorized)
    sendStatus<AuthorizationException>(HttpStatusCode.Forbidden)
    sendStatus<EntityAlreadyExistsException>(HttpStatusCode.BadRequest)
    sendStatus<BadRequestException>(HttpStatusCode.BadRequest)
    sendStatus<UserAlreadyExistException>(HttpStatusCode.BadRequest)
    sendStatus<OrganizationAlreadyExistException>(HttpStatusCode.BadRequest)
    sendStatus<EntityNotFoundException>(HttpStatusCode.NotFound)
    sendStatus<UserNotFoundException>(HttpStatusCode.NotFound)
    sendStatus<PasscodeExpiredException>(HttpStatusCode.NotFound)
    sendStatus<PasscodeLimitExceededException>(HttpStatusCode.TooManyRequests)
    sendStatus<UnsupportedJwtException>(HttpStatusCode.BadRequest)
    sendStatus<MalformedJwtException>(HttpStatusCode.BadRequest)
    sendStatus<SignatureException>(HttpStatusCode.BadRequest)
    sendStatus<ExpiredJwtException>(HttpStatusCode.Unauthorized)
    sendStatus<PolicyFormatException>(HttpStatusCode.BadRequest)
    sendStatus<InternalException>(HttpStatusCode.InternalServerError, true)
    sendStatus<DeleteOrUpdateWithoutWhereException>(HttpStatusCode.InternalServerError)
    sendStatus<HrnParseException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalAccessException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalStateException>(HttpStatusCode.InternalServerError, true)
    sendStatus<IllegalArgumentException>(HttpStatusCode.BadRequest)
    sendStatus<DataAccessException>(HttpStatusCode.InternalServerError, true)
    sendStatus<UnknownException>(HttpStatusCode.InternalServerError)
    sendStatus<Throwable>(HttpStatusCode.InternalServerError, true, "Internal Server Error Occurred")
}

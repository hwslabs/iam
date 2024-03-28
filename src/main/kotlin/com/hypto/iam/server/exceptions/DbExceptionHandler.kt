package com.hypto.iam.server.exceptions

import io.ktor.server.plugins.BadRequestException
import org.jooq.exception.DataAccessException
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object DbExceptionHandler {
    data class DbExceptionMap(
        val errorMessage: String,
        val constraintRegex: Regex,
        val appExceptions: Map<String, Pair<KClass<out Exception>, String>>,
    )

    private val customExceptions: Set<KClass<out Throwable>> =
        setOf(
            BadRequestException::class,
            EntityNotFoundException::class,
            EntityAlreadyExistsException::class,
        )
    private val duplicateConstraintRegex = "\"(.+)?\"".toRegex()
    private val foreignKeyConstraintRegex = "foreign key constraint \"(.+)?\"".toRegex()

    private val duplicateExceptions = mapOf<String, Pair<KClass<out Exception>, String>>()

    private val foreignKeyExceptions = mapOf<String, Pair<KClass<out Exception>, String>>()

    private val dbExceptionMap =
        listOf(
            DbExceptionMap(
                "duplicate key value violates unique constraint",
                duplicateConstraintRegex,
                duplicateExceptions,
            ),
            DbExceptionMap(
                "violates foreign key constraint",
                foreignKeyConstraintRegex,
                foreignKeyExceptions,
            ),
        )

    fun mapToApplicationException(e: DataAccessException): Exception {
        var finalException: Exception? = null
        val causeMessage = e.cause?.message ?: e.message

        e.cause?.let {
            if (customExceptions.contains(it::class)) {
                return it as Exception
            }
        }

        dbExceptionMap.forEach {
            if (causeMessage?.contains(it.errorMessage) == true) {
                val constraintKey = it.constraintRegex.find(causeMessage)?.groups?.get(1)?.value
                val exceptionPair = it.appExceptions[constraintKey]
                if (exceptionPair != null) {
                    finalException = exceptionPair.first.primaryConstructor?.call(exceptionPair.second, e)
                }
            }
        }

        return finalException ?: InternalException("Unknown error occurred", e)
    }
}

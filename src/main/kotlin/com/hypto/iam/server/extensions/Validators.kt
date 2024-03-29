package com.hypto.iam.server.extensions

import com.google.gson.Gson
import com.hypto.iam.server.Constants.Companion.MAX_NAME_LENGTH
import com.hypto.iam.server.Constants.Companion.MIN_LENGTH
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.HrnParseException
import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KProperty1

private val gson: Gson = getKoinInstance()

fun <T> ValidationBuilder<T>.oneOf(
    instance: T,
    values: List<KProperty1<T, *>>,
) =
    addConstraint("must have only one of the provided attributes") {
        println(values)
        return@addConstraint (values.mapNotNull { it.get(instance) }.size == 1)
    }

fun <T> ValidationBuilder<T>.oneOrMoreOf(
    instance: T,
    values: List<KProperty1<T, *>>,
) =
    addConstraint("must have at least one of the provided attributes") {
        return@addConstraint values.mapNotNull { it.get(instance) }.isNotEmpty()
    }

enum class TimeNature { ANY, PAST, FUTURE }

fun ValidationBuilder<String>.dateTime(
    format: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    nature: TimeNature = TimeNature.ANY,
) = addConstraint("must be a valid date time string") {
    try {
        val dateTime = LocalDateTime.parse(it, format)
        when (nature) {
            TimeNature.ANY -> {
                true
            }
            TimeNature.PAST -> {
                dateTime.isBefore(LocalDateTime.now())
            }
            TimeNature.FUTURE -> {
                dateTime.isAfter(LocalDateTime.now())
            }
        }
    } catch (e: DateTimeParseException) {
        false
    }
}

fun ValidationBuilder<String>.hrn() =
    addConstraint("must be a valid hrn") {
        try {
            HrnFactory.getHrn(it)
            true
        } catch (e: HrnParseException) {
            false
        }
    }

fun ValidationBuilder<String>.noEndSpaces() =
    addConstraint("must not have spaces at either ends") {
        it.trim() == it
    }

const val RESOURCE_NAME_REGEX = "^[a-zA-Z0-9_-]*\$"
const val RESOURCE_NAME_REGEX_HINT = "Only characters A..Z, a..z, 0-9, _ and - are supported."
val nameCheck =
    Validation {
        minLength(MIN_LENGTH) hint "Minimum length expected is $MIN_LENGTH"
        maxLength(MAX_NAME_LENGTH) hint "Maximum length supported for name is $MAX_NAME_LENGTH characters"
        pattern(RESOURCE_NAME_REGEX) hint RESOURCE_NAME_REGEX_HINT
    }

/**
 * Extension method to throw exception when request object don't meet the constraint.
 */
fun <T> Validation<T>.validateAndThrowOnFailure(value: T): T {
    val result = validate(value)
    if (result is Invalid<T>) {
        throw IllegalArgumentException(gson.toJson(result.errors))
    }
    return value
}

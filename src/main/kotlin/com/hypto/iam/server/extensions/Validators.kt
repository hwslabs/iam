package com.hypto.iam.server.extensions

import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KProperty1

fun <T> ValidationBuilder<T>.oneOf(instance: T, values: List<KProperty1<T, *>>) =
    addConstraint("must have only one of the provided attributes") {
        println(values)
        return@addConstraint (values.mapNotNull { it.get(instance) }.size == 1)
    }

fun <T> ValidationBuilder<T>.oneOrMoreOf(instance: T, values: List<KProperty1<T, *>>) =
    addConstraint("must have at least one of the provided attributes") {
        return@addConstraint values.mapNotNull { it.get(instance) }.isNotEmpty()
    }

enum class TimeNature { ANY, PAST, FUTURE }

fun ValidationBuilder<String>.dateTime(
    format: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    nature: TimeNature = TimeNature.ANY
) = addConstraint("must be a valid date time string") {
    try {
        val dateTime = LocalDateTime.parse(it, format)
        when (nature) {
            TimeNature.ANY -> { true }
            TimeNature.PAST -> { dateTime.isBefore(LocalDateTime.now()) }
            TimeNature.FUTURE -> { dateTime.isAfter(LocalDateTime.now()) }
        }
    } catch (e: DateTimeParseException) {
        false
    }
}

/**
 * Extension method to throw exception when request object don't meet the constraint.
 */
fun <T> Validation<T>.validateAndThrowOnFailure(value: T): T {
    val result = validate(value)
    if (result is Invalid<T>) {
        throw IllegalArgumentException(result.errors.toString())
    }
    return value
}

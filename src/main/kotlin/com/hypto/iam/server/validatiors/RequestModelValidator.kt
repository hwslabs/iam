package com.hypto.iam.server.validatiors

import com.hypto.iam.server.Constants
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.models.UpdateOrganizationRequest
import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// This file contains extension functions to valid input data given by clients

/**
 * Extension function to validate CreateOrganizationRequest input from client
 */
fun CreateOrganizationRequest.validate(): CreateOrganizationRequest {
    return Validation<CreateOrganizationRequest> {
        // Name is mandatory parameter and max length should be 50
        CreateOrganizationRequest::name required {
            minLength(Constants.MIN_NAME_LENGTH) hint "Minimum length expected is 2"
            maxLength(Constants.MAX_NAME_LENGTH) hint "Maximum length supported for name is 50 characters"
            pattern("^[a-zA-Z_]*\$") hint "Only characters A..Z, a..z & _ are supported."
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateOrganizationRequest input from client
 */
fun UpdateOrganizationRequest.validate(): UpdateOrganizationRequest {
    return Validation<UpdateOrganizationRequest> {
        UpdateOrganizationRequest::description required {
            minLength(Constants.MIN_DESC_LENGTH) hint "Minimum length expected is 2"
            maxLength(Constants.MAX_DESC_LENGTH) hint "Maximum length supported for description is 100 characters"
            pattern("^[a-zA-Z_]*\$") hint "Only characters A..Z, a..z & _ are supported."
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate CreateCredentialRequest input from client
 */
fun CreateCredentialRequest.validate(): CreateCredentialRequest {
    return Validation<CreateCredentialRequest> {
        CreateCredentialRequest::validUntil ifPresent {
            dateTime(nature = TimeNature.FUTURE)
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateCredentialRequest input from client
 */
fun UpdateCredentialRequest.validate(): UpdateCredentialRequest {
    return Validation<UpdateCredentialRequest> {
        // TODO: Implement oneOrMoreOf(UpdateCredentialRequest::status, UpdateCredentialRequest::validUntil)
        UpdateCredentialRequest::validUntil ifPresent {
            dateTime(nature = TimeNature.FUTURE)
        }
    }.validateAndThrowOnFailure(this)
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
    return if (result is Invalid<T>) {
        throw IllegalArgumentException(result.errors.toString())
    } else {
        return value
    }
}

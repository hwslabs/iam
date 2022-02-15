package com.hypto.iam.server.validatiors

import com.hypto.iam.server.Constants
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.UpdateOrganizationRequest
import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern

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

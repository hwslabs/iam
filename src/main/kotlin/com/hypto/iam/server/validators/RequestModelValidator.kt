package com.hypto.iam.server.validators

import com.hypto.iam.server.Constants
import com.hypto.iam.server.extensions.TimeNature
import com.hypto.iam.server.extensions.dateTime
import com.hypto.iam.server.extensions.oneOrMoreOf
import com.hypto.iam.server.extensions.validateAndThrowOnFailure
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.models.UpdateOrganizationRequest
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern

// This file contains extension functions to validate input data given by clients

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
        (this::oneOrMoreOf)(
            this@validate,
            listOf(UpdateCredentialRequest::status, UpdateCredentialRequest::validUntil)
        )
        UpdateCredentialRequest::validUntil ifPresent {
            dateTime(nature = TimeNature.FUTURE)
        }
    }.validateAndThrowOnFailure(this)
}

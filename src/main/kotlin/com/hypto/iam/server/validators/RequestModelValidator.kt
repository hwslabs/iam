package com.hypto.iam.server.validators

import com.hypto.iam.server.Constants.Companion.MAX_POLICY_STATEMENTS
import com.hypto.iam.server.Constants.Companion.MIN_POLICY_STATEMENTS
import com.hypto.iam.server.extensions.TimeNature
import com.hypto.iam.server.extensions.dateTime
import com.hypto.iam.server.extensions.nameCheck
import com.hypto.iam.server.extensions.oneOrMoreOf
import com.hypto.iam.server.extensions.validateAndThrowOnFailure
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.models.UpdatePolicyRequest
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxItems
import io.konform.validation.jsonschema.minItems

// This file contains extension functions to validate input data given by clients

/**
 * Extension function to validate CreateOrganizationRequest input from client
 */
fun CreateOrganizationRequest.validate(): CreateOrganizationRequest {
    return Validation<CreateOrganizationRequest> {
        // Name is mandatory parameter and max length should be 50
        CreateOrganizationRequest::name required {
            run(nameCheck)
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateOrganizationRequest input from client
 */
fun UpdateOrganizationRequest.validate(): UpdateOrganizationRequest {
    return Validation<UpdateOrganizationRequest> {
        UpdateOrganizationRequest::description required {
            run(nameCheck)
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

fun CreatePolicyRequest.validate(): CreatePolicyRequest {
    return Validation<CreatePolicyRequest> {
        CreatePolicyRequest::name required {
            run(nameCheck)
        }
        CreatePolicyRequest::statements required {
            minItems(MIN_POLICY_STATEMENTS)
            maxItems(MAX_POLICY_STATEMENTS)
        }
        CreatePolicyRequest::statements onEach {
            PolicyStatement::resourceType required { run(nameCheck) }
            PolicyStatement::action required { run(nameCheck) }
            PolicyStatement::effect required {}
        }
    }.validateAndThrowOnFailure(this)
}

fun UpdatePolicyRequest.validate(): UpdatePolicyRequest {
    return Validation<UpdatePolicyRequest> {
        UpdatePolicyRequest::statements required {
            minItems(MIN_POLICY_STATEMENTS)
            maxItems(MAX_POLICY_STATEMENTS)
        }
        UpdatePolicyRequest::statements onEach {
            PolicyStatement::resourceType required { run(nameCheck) }
            PolicyStatement::action required { run(nameCheck) }
            PolicyStatement::effect required {}
        }
    }.validateAndThrowOnFailure(this)
}

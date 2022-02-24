package com.hypto.iam.server.validators

import com.hypto.iam.server.Constants
import com.hypto.iam.server.Constants.Companion.MAX_POLICY_ASSOCIATIONS_PER_REQUEST
import com.hypto.iam.server.Constants.Companion.MAX_POLICY_STATEMENTS
import com.hypto.iam.server.Constants.Companion.MIN_POLICY_STATEMENTS
import com.hypto.iam.server.extensions.MagicNumber
import com.hypto.iam.server.extensions.TimeNature
import com.hypto.iam.server.extensions.dateTime
import com.hypto.iam.server.extensions.hrn
import com.hypto.iam.server.extensions.oneOrMoreOf
import com.hypto.iam.server.extensions.validateAndThrowOnFailure
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.ResourceAction
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.models.UpdatePolicyRequest
import com.hypto.iam.server.models.ValidationRequest
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxItems
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minItems
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
            policyStatementValidation
        }
    }.validateAndThrowOnFailure(this)
}

fun ValidationRequest.validate(): ValidationRequest {
    return validationRequestValidation.validateAndThrowOnFailure(this)
}

fun ResourceAction.validate(): ResourceAction {
    return resourceActionValidation.validateAndThrowOnFailure(this)
}

fun PolicyAssociationRequest.validate(): PolicyAssociationRequest {
    return Validation<PolicyAssociationRequest> {
        PolicyAssociationRequest::policies required {
            minItems(MagicNumber.ONE)
            maxItems(MAX_POLICY_ASSOCIATIONS_PER_REQUEST)
        }
        PolicyAssociationRequest::policies onEach { hrn() }
    }.validateAndThrowOnFailure(this)
}

// Validations used by ValidationBuilders

const val RESOURCE_NAME_REGEX = "^[a-zA-Z0-9_-]*\$"
const val RESOURCE_NAME_REGEX_HINT = "Only characters A..Z, a..z, 0-9, _ and - are supported."
val nameCheck = Validation<String> {
    minLength(Constants.MIN_NAME_LENGTH) hint "Minimum length expected is ${Constants.MIN_NAME_LENGTH}"
    maxLength(Constants.MAX_NAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_NAME_LENGTH} characters"
    pattern(RESOURCE_NAME_REGEX) hint RESOURCE_NAME_REGEX_HINT
}

val policyStatementValidation = Validation<PolicyStatement> {
    PolicyStatement::resourceType required { run(nameCheck) }
    PolicyStatement::action required { run(nameCheck) }
    PolicyStatement::effect required {}
}

val validationRequestValidation = Validation<ValidationRequest> {
    ValidationRequest::validations required {}
    ValidationRequest::validations onEach {
        resourceActionValidation
    }
}

val resourceActionValidation = Validation<ResourceAction> {
    ResourceAction::resource required { hrn() }
    ResourceAction::action required { hrn() }
}

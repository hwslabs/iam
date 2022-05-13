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
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.ResourceAction
import com.hypto.iam.server.models.RootUser
import com.hypto.iam.server.models.UpdateActionRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.models.UpdateOrganizationRequest
import com.hypto.iam.server.models.UpdatePolicyRequest
import com.hypto.iam.server.models.UpdateResourceRequest
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.ValidationRequest
import com.hypto.iam.server.models.VerifyEmailRequest
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
        CreateOrganizationRequest::rootUser required {
            run(rootUserRequestValidation)
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateOrganizationRequest input from client
 */
fun UpdateOrganizationRequest.validate(): UpdateOrganizationRequest {
    return Validation<UpdateOrganizationRequest> {
        UpdateOrganizationRequest::description required {
            run(descriptionCheck)
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

/**
 * Extension function to validate CreateResourceRequest input from client
 */
fun CreateResourceRequest.validate(): CreateResourceRequest {
    return Validation<CreateResourceRequest> {
        CreateResourceRequest::name required {
            run(nameCheck)
        }
        CreateResourceRequest::description ifPresent {
            run(descriptionCheck)
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateResourceRequest input from client
 */
fun UpdateResourceRequest.validate(): UpdateResourceRequest {
    return Validation<UpdateResourceRequest> {
        UpdateResourceRequest::description ifPresent {
            run(descriptionCheck)
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate CreateActionRequest input from client
 */
fun CreateActionRequest.validate(): CreateActionRequest {
    return Validation<CreateActionRequest> {
        CreateActionRequest::name required {
            run(nameCheck)
        }
        CreateActionRequest::description ifPresent {
            run(descriptionCheck)
        }
    }.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateActionRequest input from client
 */
fun UpdateActionRequest.validate(): UpdateActionRequest {
    return Validation<UpdateActionRequest> {
        UpdateActionRequest::description required {
            run(descriptionCheck)
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
        // TODO: [IMPORTANT] should we do a hrn check for resource and action in a policy statement?
        CreatePolicyRequest::statements onEach {
            PolicyStatement::resource required { run(hrnCheck) }
            PolicyStatement::action required { run(hrnCheck) }
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

fun CreateUserRequest.validate(): CreateUserRequest {
    return Validation<CreateUserRequest> {
        CreateUserRequest::username required {
            run(userNameCheck)
        }
        CreateUserRequest::email {
            run(emailCheck)
        }
        CreateUserRequest::passwordHash {
            run(passwordCheck)
        }
        CreateUserRequest::phone ifPresent {
            run(phoneNumberCheck)
        }
    }.validateAndThrowOnFailure(this)
}

fun UpdateUserRequest.validate(): UpdateUserRequest {
    return Validation<UpdateUserRequest> {
        UpdateUserRequest::email ifPresent {
            run(emailCheck)
        }
        UpdateUserRequest::phone ifPresent {
            run(phoneNumberCheck)
        }
    }.validateAndThrowOnFailure(this)
}

fun VerifyEmailRequest.validate(): VerifyEmailRequest {
    return Validation<VerifyEmailRequest> {
        VerifyEmailRequest::email required {
            run(emailCheck)
        }
        VerifyEmailRequest::organizationId ifPresent {
            run(organizationIdCheck)
        }
        VerifyEmailRequest::purpose required {}
    }.validateAndThrowOnFailure(this)
}

// Validations used by ValidationBuilders

const val RESOURCE_NAME_REGEX = "^[a-zA-Z0-9_-]*\$"
const val PHONE_NUMBER_REGEX = "^[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\\s\\./0-9]*\$"
const val PHONE_NUMBER_REGEX_HINT = "Only characters +, -, 0..9 are supported."
const val HRN_PREFIX_REGEX = "^hrn:[^\n]*"
const val RESOURCE_NAME_REGEX_HINT = "Only characters A..Z, a..z, 0-9, _ and - are supported."
const val EMAIL_REGEX = "^\\S+@\\S+\\.\\S+\$"
const val EMAIL_REGEX_HINT = "Email should contain `.`, `@`"
const val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@_#])[A-Za-z\\d@_#]{8,}\$"
const val PASSWORD_REGEX_HINT = "Password should contain at least one uppercase letter, " +
    "one lowercase letter, one number and one special character[@ _ #]"
const val ORGANIZATION_ID_REGEX = "^[a-zA-Z0-9]*\$"
const val ORGANIZATION_ID_REGEX_HINT = "Organization ID should be a valid alphanumeric string"

val nameCheck = Validation<String> {
    minLength(Constants.MIN_LENGTH) hint "Minimum length expected is ${Constants.MIN_LENGTH}"
    maxLength(Constants.MAX_NAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_NAME_LENGTH} characters"
    pattern(RESOURCE_NAME_REGEX) hint RESOURCE_NAME_REGEX_HINT
}

val phoneNumberCheck = Validation<String> {
    minLength(Constants.MINIMUM_PHONE_NUMBER_LENGTH) hint "Minimum length expected " +
        "is ${Constants.MINIMUM_PHONE_NUMBER_LENGTH}"
    pattern(PHONE_NUMBER_REGEX) hint PHONE_NUMBER_REGEX_HINT
}
val userNameCheck = Validation<String> {
    minLength(Constants.MIN_USERNAME_LENGTH) hint "Minimum length expected is ${Constants.MIN_USERNAME_LENGTH}"
    maxLength(Constants.MAX_USERNAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_USERNAME_LENGTH} characters"
    pattern(RESOURCE_NAME_REGEX) hint RESOURCE_NAME_REGEX_HINT
}

val emailCheck = Validation<String> {
    minLength(Constants.MIN_EMAIL_LENGTH) hint "Minimum length expected is ${Constants.MIN_EMAIL_LENGTH}"
    pattern(EMAIL_REGEX) hint EMAIL_REGEX_HINT
}

val descriptionCheck = Validation<String> {
    maxLength(Constants.MAX_DESC_LENGTH) hint "Maximum length supported for" +
        "description is ${Constants.MAX_DESC_LENGTH} characters"
}

val hrnCheck = Validation<String> {
    pattern(HRN_PREFIX_REGEX) hint "HRN must start with a valid organization Id"
}

val organizationIdCheck = Validation<String> {
    pattern(ORGANIZATION_ID_REGEX) hint ORGANIZATION_ID_REGEX_HINT
}

val policyStatementValidation = Validation<PolicyStatement> {
    PolicyStatement::resource required { run(hrnCheck) }
    PolicyStatement::action required { run(hrnCheck) }
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
val passwordCheck = Validation<String> {
    minLength(Constants.MINIMUM_PASSWORD_LENGTH) hint "Minimum length expected is ${Constants.MINIMUM_PASSWORD_LENGTH}"
    pattern(PASSWORD_REGEX) hint PASSWORD_REGEX_HINT
}
val rootUserRequestValidation = Validation<RootUser> {
    RootUser::username required {
        run(userNameCheck)
    }
    RootUser::phone required {
        run(phoneNumberCheck)
    }
    RootUser::email required {
        run(emailCheck)
    }
    RootUser::passwordHash required {
        run(passwordCheck)
    }
}

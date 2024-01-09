package com.hypto.iam.server.validators

import com.hypto.iam.server.Constants
import com.hypto.iam.server.Constants.Companion.MAX_POLICY_ASSOCIATIONS_PER_REQUEST
import com.hypto.iam.server.Constants.Companion.MAX_POLICY_STATEMENTS
import com.hypto.iam.server.Constants.Companion.MAX_SUB_ORG_LENGTH
import com.hypto.iam.server.Constants.Companion.MIN_POLICY_STATEMENTS
import com.hypto.iam.server.extensions.MagicNumber
import com.hypto.iam.server.extensions.TimeNature
import com.hypto.iam.server.extensions.dateTime
import com.hypto.iam.server.extensions.hrn
import com.hypto.iam.server.extensions.noEndSpaces
import com.hypto.iam.server.extensions.oneOrMoreOf
import com.hypto.iam.server.extensions.validateAndThrowOnFailure
import com.hypto.iam.server.models.ChangeUserPasswordRequest
import com.hypto.iam.server.models.CreateActionRequest
import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.CreateOrganizationRequest
import com.hypto.iam.server.models.CreatePolicyRequest
import com.hypto.iam.server.models.CreateResourceRequest
import com.hypto.iam.server.models.CreateSubOrganizationRequest
import com.hypto.iam.server.models.CreateUserPasswordRequest
import com.hypto.iam.server.models.CreateUserRequest
import com.hypto.iam.server.models.GetDelegateTokenRequest
import com.hypto.iam.server.models.PolicyAssociationRequest
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.ResendInviteRequest
import com.hypto.iam.server.models.ResetPasswordRequest
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
import com.hypto.iam.server.security.UsernamePasswordCredential
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxItems
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minItems
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import io.konform.validation.onEach

// This file contains extension functions to validate input data given by clients

/**
 * Extension function to validate CreateOrganizationRequest input from client
 */
fun CreateOrganizationRequest.validate(): CreateOrganizationRequest {
    return createOrganizationRequestValidation.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateOrganizationRequest input from client
 */
fun UpdateOrganizationRequest.validate(): UpdateOrganizationRequest {
    return updateOrganizationRequestValidation.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate CreateCredentialRequest input from client
 */
fun CreateCredentialRequest.validate(): CreateCredentialRequest {
    return createCredentialRequestValidation.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateCredentialRequest input from client
 */
fun UpdateCredentialRequest.validate(): UpdateCredentialRequest {
    return Validation {
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
    return createResourceRequestValidation.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateResourceRequest input from client
 */
fun UpdateResourceRequest.validate(): UpdateResourceRequest {
    return updateResourceRequestValidation.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate CreateActionRequest input from client
 */
fun CreateActionRequest.validate(): CreateActionRequest {
    return createActionRequestValidation.validateAndThrowOnFailure(this)
}

/**
 * Extension function to validate UpdateActionRequest input from client
 */
fun UpdateActionRequest.validate(): UpdateActionRequest {
    return updateActionRequestValidation.validateAndThrowOnFailure(this)
}

fun CreatePolicyRequest.validate(): CreatePolicyRequest {
    return createPolicyRequestValidation.validateAndThrowOnFailure(this)
}

fun UpdatePolicyRequest.validate(): UpdatePolicyRequest {
    return updatePolicyRequestValidation.validateAndThrowOnFailure(this)
}

fun ValidationRequest.validate(): ValidationRequest {
    return validationRequestValidation.validateAndThrowOnFailure(this)
}

fun ResourceAction.validate(): ResourceAction {
    return resourceActionValidation.validateAndThrowOnFailure(this)
}

fun PolicyAssociationRequest.validate(): PolicyAssociationRequest {
    return policyAssociationRequestValidation.validateAndThrowOnFailure(this)
}

fun CreateUserRequest.validate(): CreateUserRequest {
    return createUserRequestValidation.validateAndThrowOnFailure(this)
}

fun CreateSubOrganizationRequest.validate(): CreateSubOrganizationRequest {
    // TODO: Add validation for sub organization
    return createSubOrganizationRequest.validateAndThrowOnFailure(this)
}

fun UpdateUserRequest.validate(): UpdateUserRequest {
    return updateUserRequestValidation.validateAndThrowOnFailure(this)
}

fun ChangeUserPasswordRequest.validate(): ChangeUserPasswordRequest {
    return changeUserPasswordRequestValidation.validateAndThrowOnFailure(this)
}

fun CreateUserPasswordRequest.validate(): CreateUserPasswordRequest {
    return createUserPasswordRequestValidation.validateAndThrowOnFailure(this)
}

fun ResetPasswordRequest.validate(): ResetPasswordRequest {
    return Validation {
        ResetPasswordRequest::email required {
            run(emailCheck)
        }
        ResetPasswordRequest::password required {
            run(passwordCheck)
        }
    }.validateAndThrowOnFailure(this)
}

fun VerifyEmailRequest.validate(): VerifyEmailRequest {
    verifyEmailRequestValidation.validateAndThrowOnFailure(this)

    if (purpose == VerifyEmailRequest.Purpose.signup) {
        metadata?.let { validateSignupMetadata(metadata) }
    }
    if (purpose == VerifyEmailRequest.Purpose.invite) {
        requireNotNull(organizationId) { "organizationId is required for invite purpose" }
        requireNotNull(metadata) { "metadata is required for invite purpose" }
        validateInviteMetadata(metadata)
    }

    return this
}

fun ResendInviteRequest.validate(): ResendInviteRequest {
    return resendInviteRequestValidation.validateAndThrowOnFailure(this)
}

fun UsernamePasswordCredential.validate(): UsernamePasswordCredential {
    return usernamePasswordCredentialValidation.validateAndThrowOnFailure(this)
}

fun GetDelegateTokenRequest.validate(): GetDelegateTokenRequest {
    return getDelegateTokenRequestValidation.validateAndThrowOnFailure(this)
}

// Validations used by ValidationBuilders

const val RESOURCE_NAME_REGEX = "^[a-zA-Z0-9_-]*\$"
const val RESOURCE_NAME_REGEX_HINT = "Only characters A..Z, a..z, 0-9, _ and - are supported."
const val PREFERRED_USERNAME_REGEX = "^[a-zA-Z0-9_]*\$"
const val PREFERRED_USERNAME_REGEX_HINT = "Only characters A..Z, a..z, 0-9, _ are supported."
const val PHONE_NUMBER_REGEX = "^[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\\s\\./0-9]*\$"
const val PHONE_NUMBER_REGEX_HINT = "Only characters +, -, 0..9 are supported."
const val HRN_PREFIX_REGEX = "^hrn:[^\n]*"
const val EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
const val EMAIL_REGEX_HINT = "Email should be valid and contain `.`, `@`"
const val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w]).*\$"
const val PASSWORD_REGEX_HINT = "Password should contain at least one uppercase letter, " +
    "one lowercase letter, one number and one special character"
const val ORGANIZATION_ID_REGEX = "^[a-zA-Z0-9]*\$"
const val ORGANIZATION_ID_REGEX_HINT = "Organization ID should be a valid alphanumeric string"
const val ORGANIZATION_NAME_REGEX = "^[_a-zA-Z0-9-\\s]*\$"
const val ORGANIZATION_NAME_REGEX_HINT = "Only characters A..Z, a..z, 0-9, _, - and [space] are supported"
const val CREDENTIALS_EMAIL_INVALID = "Invalid Email"
const val CREDENTIALS_PASSWORD_INVALID = "Invalid Password"

val resourceNameCheck = Validation {
    minLength(Constants.MIN_LENGTH) hint "Minimum length expected is ${Constants.MIN_LENGTH}"
    maxLength(Constants.MAX_NAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_NAME_LENGTH} characters"
    pattern(RESOURCE_NAME_REGEX) hint RESOURCE_NAME_REGEX_HINT
}

val orgNameCheck = Validation {
    minLength(Constants.MIN_LENGTH) hint "Minimum length expected is ${Constants.MIN_LENGTH}"
    maxLength(Constants.MAX_NAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_NAME_LENGTH} characters"
    pattern(ORGANIZATION_NAME_REGEX) hint ORGANIZATION_NAME_REGEX_HINT
}

val nameOfUserCheck = Validation {
    minLength(Constants.MIN_LENGTH) hint "Minimum length expected is ${Constants.MIN_LENGTH}"
    maxLength(Constants.MAX_NAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_NAME_LENGTH} characters"
    noEndSpaces()
}

val phoneNumberCheck = Validation {
    minLength(Constants.MINIMUM_PHONE_NUMBER_LENGTH) hint "Minimum length expected " +
        "is ${Constants.MINIMUM_PHONE_NUMBER_LENGTH}"
    pattern(PHONE_NUMBER_REGEX) hint PHONE_NUMBER_REGEX_HINT
}

val preferredUserNameCheck = Validation {
    minLength(Constants.MIN_USERNAME_LENGTH) hint "Minimum length expected is ${Constants.MIN_USERNAME_LENGTH}"
    maxLength(Constants.MAX_USERNAME_LENGTH) hint "Maximum length supported for" +
        "name is ${Constants.MAX_USERNAME_LENGTH} characters"
    pattern(PREFERRED_USERNAME_REGEX) hint PREFERRED_USERNAME_REGEX_HINT
}

val emailCheck = Validation {
    minLength(Constants.MIN_EMAIL_LENGTH) hint "Minimum length expected is ${Constants.MIN_EMAIL_LENGTH}"
    pattern(EMAIL_REGEX) hint EMAIL_REGEX_HINT
}

val descriptionCheck = Validation {
    maxLength(Constants.MAX_DESC_LENGTH) hint "Maximum length supported for" +
        "description is ${Constants.MAX_DESC_LENGTH} characters"
}

val hrnCheck = Validation {
    pattern(HRN_PREFIX_REGEX) hint "HRN must start with a valid organization Id"
}

val organizationIdCheck = Validation {
    pattern(ORGANIZATION_ID_REGEX) hint ORGANIZATION_ID_REGEX_HINT
}

val policyStatementValidation = Validation {
    PolicyStatement::resource required { run(hrnCheck) }
    PolicyStatement::action required { run(hrnCheck) }
    PolicyStatement::effect required {}
}

val validationRequestValidation = Validation {
    ValidationRequest::validations required {}
    ValidationRequest::validations onEach {
        resourceActionValidation
    }
}

val resourceActionValidation = Validation {
    ResourceAction::resource required { hrn() }
    ResourceAction::action required { hrn() }
}
val passwordCheck = Validation {
    minLength(Constants.MINIMUM_PASSWORD_LENGTH) hint "Minimum length expected is ${Constants.MINIMUM_PASSWORD_LENGTH}"
    pattern(PASSWORD_REGEX) hint PASSWORD_REGEX_HINT
}
val rootUserRequestValidation = Validation {
    RootUser::preferredUsername ifPresent {
        run(preferredUserNameCheck)
    }
    RootUser::name ifPresent {
        run(nameOfUserCheck)
    }
    RootUser::phone ifPresent {
        run(phoneNumberCheck)
    }
    RootUser::email required {
        run(emailCheck)
    }
    RootUser::password required {
        run(passwordCheck)
    }
}

val credentialPasswordCheck = Validation {
    minLength(Constants.MINIMUM_PASSWORD_LENGTH) hint CREDENTIALS_PASSWORD_INVALID
}

// Request model validations

val createOrganizationRequestValidation = Validation {
    // Name is mandatory parameter and max length should be 50
    CreateOrganizationRequest::name ifPresent {
        run(orgNameCheck)
    }
    CreateOrganizationRequest::rootUser ifPresent {
        run(rootUserRequestValidation)
    }
}

val updateOrganizationRequestValidation = Validation {
    UpdateOrganizationRequest::name ifPresent {
        run(orgNameCheck)
    }
    UpdateOrganizationRequest::description ifPresent {
        run(descriptionCheck)
    }
}

val createCredentialRequestValidation = Validation {
    CreateCredentialRequest::validUntil ifPresent {
        dateTime(nature = TimeNature.FUTURE)
    }
}

val createResourceRequestValidation = Validation {
    CreateResourceRequest::name required {
        run(resourceNameCheck)
    }
    CreateResourceRequest::description ifPresent {
        run(descriptionCheck)
    }
}

val updateResourceRequestValidation = Validation {
    UpdateResourceRequest::description ifPresent {
        run(descriptionCheck)
    }
}

val createActionRequestValidation = Validation {
    CreateActionRequest::name required {
        run(resourceNameCheck)
    }
    CreateActionRequest::description ifPresent {
        run(descriptionCheck)
    }
}

val updateActionRequestValidation = Validation {
    UpdateActionRequest::description required {
        run(descriptionCheck)
    }
}

val createPolicyRequestValidation = Validation {
    CreatePolicyRequest::name required {
        run(resourceNameCheck)
    }
    CreatePolicyRequest::description ifPresent {
        run(descriptionCheck)
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
}

val updatePolicyRequestValidation = Validation {
    UpdatePolicyRequest::description ifPresent {
        run(descriptionCheck)
    }
    UpdatePolicyRequest::statements required {
        minItems(MIN_POLICY_STATEMENTS)
        maxItems(MAX_POLICY_STATEMENTS)
    }
    UpdatePolicyRequest::statements ifPresent {
        onEach {
            policyStatementValidation
        }
    }
}

val policyAssociationRequestValidation = Validation {
    PolicyAssociationRequest::policies required {
        minItems(MagicNumber.ONE)
        maxItems(MAX_POLICY_ASSOCIATIONS_PER_REQUEST)
    }
    PolicyAssociationRequest::policies onEach { hrn() }
}

val createUserRequestValidation = Validation<CreateUserRequest> {
    addConstraint("Email and password is mandatory for Users with loginAccess") {
        return@addConstraint (
            (it.loginAccess == true && !it.email.isNullOrEmpty() && !it.password.isNullOrEmpty()) ||
                !(it.loginAccess ?: false)
            )
    }

    addConstraint("Password is not required for Users without loginAccess") {
        return@addConstraint (
            (
                !(it.loginAccess ?: false) && it.password.isNullOrEmpty()
                ) || (it.loginAccess == true)
            )
    }

    CreateUserRequest::preferredUsername ifPresent {
        run(preferredUserNameCheck)
    }
    CreateUserRequest::name required {
        run(nameOfUserCheck)
    }
    CreateUserRequest::email ifPresent {
        run(emailCheck)
    }
    CreateUserRequest::password ifPresent {
        run(passwordCheck)
    }
    CreateUserRequest::phone ifPresent {
        run(phoneNumberCheck)
    }
}

val updateUserRequestValidation = Validation {
    UpdateUserRequest::name ifPresent {
        run(nameOfUserCheck)
    }
    UpdateUserRequest::email ifPresent {
        run(emailCheck)
    }
    UpdateUserRequest::phone ifPresent {
        run(phoneNumberCheck)
    }
}

val createSubOrganizationRequest = Validation {
    CreateSubOrganizationRequest::name required {
        maxLength(MAX_SUB_ORG_LENGTH) hint "Maximum length supported for" +
            "name is $MAX_SUB_ORG_LENGTH characters"
        noEndSpaces()
    }
    CreateSubOrganizationRequest::description ifPresent {
        run(descriptionCheck)
    }
}

val changeUserPasswordRequestValidation = Validation {
    ChangeUserPasswordRequest::oldPassword required {
        run(passwordCheck)
    }
    ChangeUserPasswordRequest::newPassword required {
        run(passwordCheck)
    }
}

val createUserPasswordRequestValidation = Validation {
    CreateUserPasswordRequest::password required {
        run(passwordCheck)
    }
}

val verifyEmailRequestValidation = Validation {
    VerifyEmailRequest::email required {
        run(emailCheck)
    }
    VerifyEmailRequest::organizationId ifPresent {
        run(organizationIdCheck)
    }
    VerifyEmailRequest::purpose required {}
}

val resendInviteRequestValidation = Validation {
    ResendInviteRequest::email required {
        run(emailCheck)
    }
}

val usernamePasswordCredentialValidation = Validation {
    UsernamePasswordCredential::username required {
        minLength(Constants.MIN_LENGTH) hint "Minimum length expected is ${Constants.MIN_LENGTH}"
    }
    UsernamePasswordCredential::password required {
        run(credentialPasswordCheck)
    }
}

val getDelegateTokenRequestValidation = Validation {
    GetDelegateTokenRequest::policy required {}
}

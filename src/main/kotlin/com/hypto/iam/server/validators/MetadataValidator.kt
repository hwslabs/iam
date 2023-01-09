package com.hypto.iam.server.validators

import com.google.gson.Gson
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.extensions.hrn
import com.hypto.iam.server.extensions.validateAndThrowOnFailure
import io.konform.validation.Validation

// This file contains extension functions to validate provided metadata
private val gson: Gson = getKoinInstance()

data class SignUpMetadata(
    val name: String,
    val description: String?,
    val rootUserPassword: String,
    val rootUserName: String?,
    val rootUserPreferredUsername: String?,
    val rootUserPhone: String?
)

data class InviteMetadata(val map: Map<String, Any>) {
    val inviterUserHrn: String by map
    val policies: List<String> by map
}

data class VerifyEmailSignUpMetadata(
    val metadata: SignUpMetadata
)

data class VerifyEmailInviteMetadata(
    val metadata: InviteMetadata
)

val signUpMetadataValidation = Validation {
    VerifyEmailSignUpMetadata::metadata {
        SignUpMetadata::name required {
            run(orgNameCheck)
        }
        SignUpMetadata::rootUserPassword required {
            run(passwordCheck)
        }
        SignUpMetadata::rootUserName ifPresent {
            run(nameOfUserCheck)
        }
        SignUpMetadata::rootUserPreferredUsername ifPresent {
            run(preferredUserNameCheck)
        }
        SignUpMetadata::rootUserPhone ifPresent {
            run(phoneNumberCheck)
        }
    }
}

val inviteMetadataValidation = Validation {
    InviteMetadata::inviterUserHrn required {
        run(hrnCheck)
    }
    InviteMetadata::policies onEach { hrn() }
}

fun validateSignupMetadata(metadata: Map<String, Any>) {
    val metadataObject = gson.fromJson(gson.toJsonTree(metadata), SignUpMetadata::class.java)
    signUpMetadataValidation.validateAndThrowOnFailure(VerifyEmailSignUpMetadata(metadataObject))
}

fun validateInviteMetadata(metadata: Map<String, Any>) {
    val metadataObject = InviteMetadata(metadata)
    inviteMetadataValidation.validateAndThrowOnFailure(metadataObject)
}

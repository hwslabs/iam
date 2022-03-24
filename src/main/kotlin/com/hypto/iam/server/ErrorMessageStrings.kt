package com.hypto.iam.server

object ErrorMessageStrings {
    // JWT Validation Errors
    const val JWT_INVALID_ISSUER = "Invalid Issuer in JWT"
    const val JWT_INVALID_USER_HRN = "Invalid User HRN in JWT"
    const val JWT_INVALID_ORGANIZATION = "Organization not present in JWT"
    const val JWT_INVALID_VERSION_NUMBER = "Version Number not present in JWT"
    const val JWT_INVALID_ISSUED_AT = "JWT Issued At date - %s is invalid"
}

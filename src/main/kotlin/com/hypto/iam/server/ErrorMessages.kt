package com.hypto.iam.server

class ErrorMessages private constructor() {
    companion object {
        const val JWT_INVALID_ISSUER = "Invalid Issuer in JWT token"
        const val JWT_INVALID_USER_HRN = "Invalid User HRN in JWT token"
        const val JWT_INVALID_ORGANIZATION = "Organization not present in JWT token"
        const val JWT_INVALID_VERSION_NUMBER = "Version Number not present in JWT token"
        const val JWT_INVALID_ISSUED_AT = "Issued At date is invalid in JWT token %s"
        const val JWT_EXPIRED = "Jwt token expired on %s"
    }
}

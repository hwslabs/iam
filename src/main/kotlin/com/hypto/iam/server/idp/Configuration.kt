package com.hypto.iam.server.idp

data class Configuration(val passwordPolicy: PasswordPolicy)

data class PasswordPolicy(
    val minLength: Int = 8,
    val requireUpperCase: Boolean = true,
    val requireLowerCase: Boolean = true,
    val requireNumber: Boolean = true,
    val requireSymbols: Boolean = true,
)

// TODO: Add configurations related to MFA, Email verification flows

package com.hypto.iam.server.utils.jwt

data class JwtToken(
    val iss: String,
    val sub: String,
    val exp: Int,
    val iat: Int,
    val aud: String,
    val ver: String,
    val jti: String,
    val usr: String,
    val entitlements: String // content from PolicyBuilder.toString()
)

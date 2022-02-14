@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
// https://dev.to/h3xstream/how-to-solve-symbol-is-declared-in-module-x-which-does-not-export-package-y-303g

package com.hypto.iam.server.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.time.Instant
import java.util.Date
import sun.security.ec.ECPrivateKeyImpl
import sun.security.util.DerValue

class TokenService {
    companion object {
        const val ISSUER = "https://iam.hypto.com"
        const val TOKEN_VALIDITY: Long = 3600 // in seconds
        const val VERSION_CLAIM = "ver"
        const val USER_CLAIM = "usr"
        const val ORGANIZATION_CLAIM = "org"
        const val ENTITLEMENTS_CLAIM = "entitlements"
//        var publicKeyString: String = ""
        var privateKeyString: String = ""
//
//        var keyPair: KeyPair = KeyPair(ECPublicKeyImpl.parse(DerValue(publicKeyString)), ECPrivateKeyImpl.parseKey(DerValue(privateKeyString)))
        val privateKey = ECPrivateKeyImpl.parseKey(DerValue(privateKeyString))
    }

    fun generateJwtToken(userId: String, orgId: String, entitlements: String): String {
        // 1. Validate user
        // 2. get policies attached to the user
        // 3. get statements for each of the policies
        // 4. add the statements to jwt entitlements

        val builder = Jwts.builder()
            .setIssuer(ISSUER)
            .setIssuedAt(Date())
//            .setSubject("")
            .setExpiration(Date.from(Instant.now().plusSeconds(TOKEN_VALIDITY)))
//            .setAudience("")
//            .setId("")
            .claim(VERSION_CLAIM, "1.0")
            .claim(USER_CLAIM, userId.toString()) // UserId
            .claim(ORGANIZATION_CLAIM, orgId) // OrganizationId
//            .claim(ENTITLEMENTS_CLAIM, fetchEntitlements(userId))
            .signWith(privateKey, SignatureAlgorithm.ES256)
            .compact()
        return ""
    }

//    fun fetchEntitlements(userId: String): String {
//        val policies = UserPoliciesRepo.fetchByPrincipalHrn(userId)
//        PoliciesRepo.g
//    }
}

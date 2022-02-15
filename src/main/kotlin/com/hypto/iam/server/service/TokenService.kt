@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
// https://dev.to/h3xstream/how-to-solve-symbol-is-declared-in-module-x-which-does-not-export-package-y-303g

package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.EcKeysRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Date

object TokenService {
    private const val ISSUER = "https://iam.hypto.com"
    private const val TOKEN_VALIDITY: Long = 3600 // in seconds
    private const val VERSION_CLAIM = "ver"
    private const val USER_CLAIM = "usr"
    private const val ORGANIZATION_CLAIM = "org"
    private const val ENTITLEMENTS_CLAIM = "entitlements"

    var privateKey: PrivateKey = AssymetricKey.forSigning().privateKey

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
            .claim(ENTITLEMENTS_CLAIM, "fetchEntitlements(userId)")
            .signWith(privateKey, SignatureAlgorithm.ES256)
            .compact()
        return builder
    }

    fun fetchEntitlements(organizationId: String, userId: String): String {
        val userHrn = Hrn(organizationId, IamResourceTypes.USER, userId).toString()
        val policies = UserPoliciesRepo.fetchByPrincipalHrn(userHrn)
//        PoliciesRepo.fetchByOrganizationIdAndNamesIn()
        // TODO: Implement this method
        return ""
    }
}

/**
Generate EC key pair using the below commands:
- Create a ES256 private key pem:
`openssl ecparam -name prime256v1 -genkey -noout -out private_key.pem`
- Generate corresponding  public key pem:
`openssl ec -in private_key.pem -pubout -out public_key.pem`
- Generate private_key.der corresponding to private_key.pem
`openssl pkcs8 -topk8 -inform PEM -outform DER -in  private_key.pem -out  private_key.der -nocrypt`
- Generate public_key.der corresponding to private_key.pem
`openssl ec -in private_key.pem -pubout -outform DER -out public_key.der`
 * */
class AssymetricKey(private val privateKeyByteArray: ByteArray, private val publicKeyByteArray: ByteArray) {
    private val keyFactory: KeyFactory = KeyFactory.getInstance("EC")

    val publicKey: PublicKey
        get() {
            val keySpec = X509EncodedKeySpec(publicKeyByteArray)
            return keyFactory.generatePublic(keySpec)
        }
    val privateKey: PrivateKey
        get() {
            val keySpec = PKCS8EncodedKeySpec(privateKeyByteArray)
            return keyFactory.generatePrivate(keySpec)
        }

    companion object {
        fun forSigning(): AssymetricKey {
            val key = if (!::signKey.isInitialized || shouldRefreshSignKey()) {
                EcKeysRepo.fetchForSigning()
            } else {
                throw InternalException("Signing key not found")
            }

            if (key == null) {
                throw InternalException("Signing key not found")
            }

            signKeyFetchTime = Instant.now()
            signKey = AssymetricKey(key.privateKey, key.publicKey)
            return signKey
        }

        fun of(keyId: String): AssymetricKey {
            val key = EcKeysRepo.fetchById(keyId) ?: throw InternalException("Key [$keyId] not found")
            return AssymetricKey(key.privateKey, key.publicKey)
        }

        fun of(privateKeyFile: String, publicKeyFile: String): AssymetricKey {
            return AssymetricKey(readResourceFileAsBytes(privateKeyFile), readResourceFileAsBytes(publicKeyFile))
        }

        fun of(privateKeyByteArray: ByteArray, publicKeyByteArray: ByteArray): AssymetricKey {
            return AssymetricKey(privateKeyByteArray, publicKeyByteArray)
        }

        // TODO: Move to config file
        private const val cacheDuration: Long = 300 // seconds
        private lateinit var signKeyFetchTime: Instant
        private lateinit var signKey: AssymetricKey

        private fun shouldRefreshSignKey(): Boolean {
            return signKeyFetchTime.plusSeconds(cacheDuration) > Instant.now()
        }

        private fun readResourceFileAsBytes(name: String): ByteArray {
            val fileUri: URI = this::class.java.classLoader.getResource(name).toURI()
            return Files.readAllBytes(Paths.get(fileUri))
        }
    }
}

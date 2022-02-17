package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.policy.PolicyBuilder
import com.hypto.iam.server.utils.policy.PolicyStatement
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
import mu.KotlinLogging

val logger = KotlinLogging.logger("service.TokenService")

object TokenService {
    private const val ISSUER = "https://iam.hypto.com"

    // TODO: Move to config file
    const val TOKEN_VALIDITY: Long = 300 // in seconds
    private const val VERSION_CLAIM = "ver"
    private const val USER_CLAIM = "usr"
    private const val ORGANIZATION_CLAIM = "org"
    private const val ENTITLEMENTS_CLAIM = "entitlements"
    private const val KEY_ID = "kid"

    val keyPair = CachedMasterKey.forSigning()

    fun generateJwtToken(userHrn: Hrn): String {
        return Jwts.builder()
            .setHeaderParam(KEY_ID, keyPair.id)
            .setIssuer(ISSUER)
            .setIssuedAt(Date())
//            .setSubject("")
            .setExpiration(Date.from(Instant.now().plusSeconds(TOKEN_VALIDITY)))
//            .setAudience("")
//            .setId("")
            .claim(VERSION_CLAIM, "1.0")
            .claim(USER_CLAIM, userHrn.resourceInstance) // UserId
            .claim(ORGANIZATION_CLAIM, userHrn.organization) // OrganizationId
            .claim(ENTITLEMENTS_CLAIM, fetchEntitlements(userHrn.toString()))
            .signWith(keyPair.privateKey, SignatureAlgorithm.ES256)
            .compact()
    }

    private fun fetchEntitlements(userHrn: String): String {
        val userPolicies = UserPoliciesRepo.fetchByPrincipalHrn(userHrn)

        val policyBuilder = PolicyBuilder()
        userPolicies.forEach {
            val policy = PoliciesRepo.fetchByHrn(it.policyHrn)!!
            logger.info { policy.statements }

            policyBuilder.withPolicy(policy).withStatement(PolicyStatement.g(userHrn, policy.hrn))
        }

        return policyBuilder.build()
    }
}

class CachedMasterKey(
    private val privateKeyByteArray: ByteArray,
    private val publicKeyByteArray: ByteArray,
    val id: String? = null
) {
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
        fun forSigning(): CachedMasterKey {
            val key = (
                if (!::signKey.isInitialized || shouldRefreshSignKey()) MasterKeysRepo.fetchForSigning() else null
                ) ?: throw InternalException("Signing key not found")

            signKeyFetchTime = Instant.now()
            signKey = CachedMasterKey(key.privateKey, key.publicKey)
            return signKey
        }

        fun of(keyId: String): CachedMasterKey {
            val key = MasterKeysRepo.fetchById(keyId) ?: throw InternalException("Key [$keyId] not found")
            return CachedMasterKey(key.privateKey, key.publicKey, key.id.toString())
        }

        fun of(privateKeyFile: String, publicKeyFile: String): CachedMasterKey {
            return CachedMasterKey(readResourceFileAsBytes(privateKeyFile), readResourceFileAsBytes(publicKeyFile))
        }

        fun of(privateKeyByteArray: ByteArray, publicKeyByteArray: ByteArray): CachedMasterKey {
            return CachedMasterKey(privateKeyByteArray, publicKeyByteArray)
        }

        private const val cacheDuration: Long = TokenService.TOKEN_VALIDITY
        private lateinit var signKeyFetchTime: Instant
        private lateinit var signKey: CachedMasterKey

        private fun shouldRefreshSignKey(): Boolean {
            return signKeyFetchTime.plusSeconds(cacheDuration) > Instant.now()
        }

        private fun readResourceFileAsBytes(name: String): ByteArray {
            val fileUri: URI = this::class.java.classLoader.getResource(name).toURI()
            return Files.readAllBytes(Paths.get(fileUri))
        }
    }
}

package com.hypto.iam.server.service

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.ResourceHrn
import io.jsonwebtoken.CompressionCodecs
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
import java.util.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

val logger = KotlinLogging.logger("service.TokenService")

/**
 * Service which holds logic related to Organization operations
 */
interface TokenService {
    suspend fun generateJwtToken(userHrn: Hrn): String
}

class TokenServiceImpl : KoinComponent, TokenService {
    private val userPolicyService: UserPolicyService by inject()
    private val appConfig: AppConfig.Config by inject()

    companion object {
        private const val ISSUER = "https://iam.hypto.com"

        private const val VERSION_CLAIM = "ver"
        private const val USER_CLAIM = "usr"
        private const val ORGANIZATION_CLAIM = "org"
        private const val ENTITLEMENTS_CLAIM = "entitlements"
        private const val KEY_ID = "kid"
        private const val VERSION_NUM = "1.0"

        val keyPair = CachedMasterKey.forSigning()
    }

    override suspend fun generateJwtToken(userHrn: Hrn): String {
        require(userHrn is ResourceHrn) { "The input hrn must be a userHrn" }
        return Jwts.builder()
            .setHeaderParam(KEY_ID, keyPair.id)
            .setIssuer(ISSUER)
            .setIssuedAt(Date())
//            .setSubject("")
            .setExpiration(Date.from(Instant.now().plusSeconds(appConfig.app.jwtTokenValidity)))
//            .setAudience("")
//            .setId("")
            .claim(VERSION_CLAIM, VERSION_NUM)
            .claim(USER_CLAIM, userHrn.resourceInstance) // UserId
            .claim(ORGANIZATION_CLAIM, userHrn.organization) // OrganizationId
            .claim(ENTITLEMENTS_CLAIM, userPolicyService.fetchEntitlements(userHrn.toString()).toString())
            .signWith(keyPair.privateKey, SignatureAlgorithm.ES256)
            // TODO: Uncomment before taking to prod
            // Eventually move to Brotli from GZIP:
            // https://tech.oyorooms.com/how-brotli-compression-gave-us-37-latency-improvement-14d41e50fee4
            .compressWith(CompressionCodecs.GZIP)
            .compact()
    }
}

class CachedMasterKey(
    private val privateKeyByteArray: ByteArray,
    private val publicKeyByteArray: ByteArray,
    val id: String? = null
) : KoinComponent {
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

        private lateinit var signKeyFetchTime: Instant
        private lateinit var signKey: CachedMasterKey
        private val appConfig = getKoinInstance<AppConfig.Config>()

        private fun shouldRefreshSignKey(): Boolean {
            return signKeyFetchTime.plusSeconds(appConfig.app.jwtTokenValidity) > Instant.now()
        }

        private fun readResourceFileAsBytes(name: String): ByteArray {
            val fileUri: URI = this::class.java.classLoader.getResource(name).toURI()
            return Files.readAllBytes(Paths.get(fileUri))
        }
    }
}

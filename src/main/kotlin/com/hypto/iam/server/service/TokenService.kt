package com.hypto.iam.server.service

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.tables.pojos.MasterKeys
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.exceptions.JwtExpiredException
import com.hypto.iam.server.models.TokenResponse
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.ResourceHrn
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SigningKeyResolverAdapter
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

val logger = KotlinLogging.logger("service.TokenService")

/**
 * Service which holds logic related to Organization operations
 */
interface TokenService {
    suspend fun generateJwtToken(userHrn: Hrn): TokenResponse
//    suspend fun parseHeader(token: String): JwtHeader
    suspend fun validateJwtToken(token: String): Jws<Claims>
}

class TokenServiceImpl : KoinComponent, TokenService {
    private val userPolicyService: UserPolicyService by inject()
    private val appConfig: AppConfig.Config by inject()
    private val masterKeyCache: MasterKeyCache by inject()

    companion object {
        private const val ISSUER = "https://iam.hypto.com"

        private const val VERSION_CLAIM = "ver"
        const val USER_CLAIM = "usr"
        private const val ORGANIZATION_CLAIM = "org"
        private const val ENTITLEMENTS_CLAIM = "entitlements"
        private const val KEY_ID = "kid"
        private const val VERSION_NUM = "1.0"
    }

    /**
     * Validates the following:
     * - signature of the JWT token
     * - expiry claim
     * - version claim (TODO)
     *
     * @throws io.jsonwebtoken.UnsupportedJwtException
     * @throws io.jsonwebtoken.MalformedJwtException
     * @throws io.jsonwebtoken.SignatureException
     * @throws JwtExpiredException
     * @throws IllegalArgumentException
     */
    override suspend fun validateJwtToken(token: String): Jws<Claims> {
        val jws = Jwts.parserBuilder()
            .setSigningKeyResolver(JwtSigVerificationKeyResolver)
            .build()
            .parseClaimsJws(token)

        // Validate claims
        val body = jws.body

        val issuer: String? = body.get(Claims.ISSUER, String::class.java)
        require(issuer != null && issuer == ISSUER)

        val userHrnStr: String? = body.get(USER_CLAIM, String::class.java)
        require(userHrnStr != null && HrnFactory.isValid(userHrnStr))

        val organization: String? = body.get(ORGANIZATION_CLAIM, String::class.java)
        require(organization != null)

        val expiry: Date? = body.get(Claims.EXPIRATION, Date::class.java)
        if (!(expiry is Date && expiry.toInstant() > Instant.now())) {
            throw JwtExpiredException("Jwt token expired on $expiry")
        }

        val versionNum: String? = body.get(VERSION_CLAIM, String::class.java)
        require(versionNum != null)

        // TODO: Validate issued_at and entitlement claims
        // body[ENTITLEMENTS_CLAIM]

        return jws
    }

    override suspend fun generateJwtToken(userHrn: Hrn): TokenResponse {
        require(userHrn is ResourceHrn) { "The input hrn must be a userHrn" }
        val signingKey = masterKeyCache.forSigning()

        return TokenResponse(
            Jwts.builder()
                .setHeaderParam(KEY_ID, signingKey.id)
                .setIssuer(ISSUER)
                .setIssuedAt(Date())
//            .setSubject("")
                .setExpiration(Date.from(Instant.now().plusSeconds(appConfig.app.jwtTokenValidity)))
//            .setAudience("")
//            .setId("")
                .claim(VERSION_CLAIM, VERSION_NUM)
                .claim(USER_CLAIM, userHrn.toString()) // UserId
                .claim(ORGANIZATION_CLAIM, userHrn.organization) // OrganizationId
                .claim(ENTITLEMENTS_CLAIM, userPolicyService.fetchEntitlements(userHrn.toString()).toString())
                .signWith(signingKey.privateKey, SignatureAlgorithm.ES256)
                // TODO: Uncomment before taking to prod
                // Eventually move to Brotli from GZIP:
                // https://tech.oyorooms.com/how-brotli-compression-gave-us-37-latency-improvement-14d41e50fee4
                // .compressWith(CompressionCodecs.GZIP)
                .compact()
        )
    }

    object JwtSigVerificationKeyResolver : KoinComponent, SigningKeyResolverAdapter() {
        private val masterKeyCache: MasterKeyCache by inject()
        override fun resolveSigningKey(jwsHeader: JwsHeader<*>, claims: Claims): PublicKey {
            val keyId = jwsHeader.keyId
            return masterKeyCache.getKey(keyId).publicKey
        }
    }
}

object MasterKeyCache : KoinComponent {
    private val cache = ConcurrentHashMap<String, MasterKey>()

    private lateinit var signKeyFetchTime: Instant
    private lateinit var signKey: MasterKey
    private val appConfig: AppConfig.Config by inject()

    private fun shouldRefreshSignKey(): Boolean {
        return signKeyFetchTime.plusSeconds(appConfig.app.signKeyFetchInterval) > Instant.now()
    }

    fun forSigning(): MasterKey {
        if (!::signKey.isInitialized || shouldRefreshSignKey()) {
            val signKeyFromDb = MasterKey.forSigning()
            signKeyFetchTime = Instant.now()

            if (::signKey.isInitialized && signKey.id != signKeyFromDb.id) {
                // This happens in case of a recent master key rotation.
                // We invalidate cache to clear stale sign and verification key cache entries.
                cache.clear()
                cache[signKey.id] = signKey
            }
            signKey = signKeyFromDb
        }
        return signKey
    }

    fun getKey(id: String): MasterKey {
        if (cache.containsKey(id)) {
            return cache[id]!!
        }

        val masterKey = MasterKey.of(id)
        cache[id] = masterKey
        return masterKey
    }
}

class MasterKey(
    private val privateKeyByteArray: ByteArray,
    private val publicKeyByteArray: ByteArray,
    val id: String
) {
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

    companion object : KoinComponent {
        private val masterKeysRepo: MasterKeysRepo by inject()
        val keyFactory: KeyFactory = KeyFactory.getInstance("EC")
        fun forSigning(): MasterKey {
            return masterKeysRepo.fetchForSigning()?.let {
                MasterKey(it.privateKey, it.publicKey, it.id.toString())
            } ?: throw InternalException("Signing key not found")
        }

        fun of(keyId: String): MasterKey {
            val key = masterKeysRepo.fetchById(keyId) ?: throw InternalException("Key [$keyId] not found")
            return of(key)
        }

        fun of(key: MasterKeys): MasterKey {
            return MasterKey(key.privateKey, key.publicKey, key.id.toString())
        }
    }
}

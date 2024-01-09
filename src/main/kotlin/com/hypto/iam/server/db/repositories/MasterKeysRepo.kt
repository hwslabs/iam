package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS
import com.hypto.iam.server.db.tables.pojos.MasterKeys
import com.hypto.iam.server.db.tables.records.MasterKeysRecord
import com.hypto.iam.server.utils.MasterKeyUtil
import org.jooq.Configuration
import org.jooq.impl.DAOImpl
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

object MasterKeysRepo : BaseRepo<MasterKeysRecord, MasterKeys, UUID>() {
    private val appConfig: AppConfig by inject()

    private val idFun = fun (masterKey: MasterKeys): UUID {
        return masterKey.id
    }

    override suspend fun dao(): DAOImpl<MasterKeysRecord, MasterKeys, UUID> {
        return txMan.getDao(
            MASTER_KEYS,
            MasterKeys::class.java,
            idFun,
        )
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    suspend fun fetchById(value: String): MasterKeys? {
        return fetchOne(MASTER_KEYS.ID, UUID.fromString(value))
    }

    /**
     * Fetch a unique record that has `status = "SIGNING"`
     */
    suspend fun fetchForSigning(): MasterKeys? {
        return fetchOne(MASTER_KEYS.STATUS, Status.SIGNING.value)
    }

    // TODO: #1 - [IMPORTANT] Encrypt private keys stored in database with a passphrase
    // TODO: #2 - GRANT only necessary permissions for iam application user to the master_keys table
    suspend fun rotateKey(skipIfPresent: Boolean = false): Boolean {
        txMan.wrap {
            val c = dao().configuration()
            if (getMasterKeyOperationLock(c) && shouldRotate(skipIfPresent)) {
                println("Generating key pair")
                MasterKeyUtil.generateKeyPair()

                c.dsl().update(MASTER_KEYS)
                    .set(MASTER_KEYS.STATUS, Status.EXPIRED.value)
                    .where(
                        MASTER_KEYS.STATUS.eq(Status.VERIFYING.value),
                        MASTER_KEYS.UPDATED_AT.lessThan(
                            LocalDateTime.ofInstant(
                                Instant.now().minusSeconds(appConfig.app.oldKeyTtl),
                                ZoneOffset.systemDefault(),
                            ),
                        ),
                    ).execute()

                c.dsl().update(MASTER_KEYS)
                    .set(MASTER_KEYS.STATUS, Status.VERIFYING.value)
                    .where(MASTER_KEYS.STATUS.eq(Status.SIGNING.value))
                    .execute()

                c.dsl()
                    .insertInto(MASTER_KEYS)
                    .set(MASTER_KEYS.STATUS, Status.SIGNING.value)
                    .set(MASTER_KEYS.PRIVATE_KEY_PEM, MasterKeyUtil.loadPrivateKeyPem())
                    .set(MASTER_KEYS.PRIVATE_KEY_DER, MasterKeyUtil.loadPrivateKeyDer())
                    .set(MASTER_KEYS.PUBLIC_KEY_PEM, MasterKeyUtil.loadPublicKeyPem())
                    .set(MASTER_KEYS.PUBLIC_KEY_DER, MasterKeyUtil.loadPublicKeyDer())
                    .set(MASTER_KEYS.CREATED_AT, LocalDateTime.now())
                    .set(MASTER_KEYS.UPDATED_AT, LocalDateTime.now())
                    .execute()
            }
        }
        return true
    }

    /**
     * If skipIfPresent = true, returns true if master key for signing does not exist. false otherwise
     * If skipIfPresent = false, always returns true to continue with key rotation.
     */
    private suspend fun shouldRotate(skipIfPresent: Boolean): Boolean {
        return if (skipIfPresent) {
            // TODO: Convert to exists query rather than a fetchOne query
            fetchForSigning() == null
        } else {
            true
        }
    }

    private fun getMasterKeyOperationLock(c: Configuration): Boolean {
        return (
            c.dsl()
                .fetchOne("select pg_try_advisory_xact_lock($MASTER_KEY_OPERATION_LOCK_KEY) as lock")
                ?.get("lock")
                ?: false
        ) as Boolean
    }

    private const val MASTER_KEY_OPERATION_LOCK_KEY = 10

    enum class Status(val value: String) {
        SIGNING("SIGNING"),
        VERIFYING("VERIFYING"),
        EXPIRED("EXPIRED"),
    }
}

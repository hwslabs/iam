package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.MasterKeys
import com.hypto.iam.server.db.tables.records.MasterKeysRecord
import com.hypto.iam.server.utils.MasterKeyUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jooq.Configuration
import org.jooq.impl.DAOImpl

object MasterKeysRepo : DAOImpl<MasterKeysRecord, MasterKeys, UUID>(
    com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS,
    MasterKeys::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(masterKey: MasterKeys): UUID {
        return masterKey.id
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchById(value: UUID): MasterKeys? {
        return fetchOne(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.ID, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchById(value: String): MasterKeys? {
        return fetchOne(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.ID, UUID.fromString(value))
    }

    /**
     * Fetch a unique record that has `status = "SIGNING"`
     */
    fun fetchForSigning(): MasterKeys? {
        return fetchOne(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.STATUS, Status.SIGNING.value)
    }

    /**
     * Rotate master key in DB
     * @param oldKeyTtl TTL in seconds until which the rotated key must be available for verifying signatures.
     */
    // TODO: #1 - Move TTL to config file
    // TODO: #2 - Encrypt private keys stored in database with a passphrase
    // TODO: #3 - GRANT only necessary permissions for iam application user to the master_keys table
    fun rotateKey(oldKeyTtl: Long = 600 /* 2X the local cache duration */, skipIfPresent: Boolean = false): Boolean {

        ctx().transaction { c ->
            if (getMasterKeyOperationLock(c) && shouldRotate(skipIfPresent)) {

                println("Generating key pair")
                MasterKeyUtil.generateKeyPair()

                c.dsl().update(table)
                    .set(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.STATUS, Status.EXPIRED.value)
                    .where(
                        com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.STATUS.eq(Status.VERIFYING.value),
                        com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.UPDATED_AT.lessThan(
                            LocalDateTime.ofInstant(Instant.now().minusSeconds(oldKeyTtl), ZoneOffset.systemDefault())
                        )
                    ).execute()

                c.dsl().update(table)
                    .set(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.STATUS, Status.VERIFYING.value)
                    .where(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.STATUS.eq(Status.SIGNING.value))
                    .execute()

                c.dsl()
                    .insertInto(table)
                    .set(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.STATUS, Status.SIGNING.value)
                    .set(
                        com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.PRIVATE_KEY,
                        MasterKeyUtil.loadPrivateKeyDer()
                    )
                    .set(
                        com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.PUBLIC_KEY,
                        MasterKeyUtil.loadPublicKeyDer()
                    )
                    .set(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.CREATED_AT, LocalDateTime.now())
                    .set(com.hypto.iam.server.db.tables.MasterKeys.MASTER_KEYS.UPDATED_AT, LocalDateTime.now())
                    .execute()
            }
        }
        return true
    }

    /**
     * If skipIfPresent = true, returns true if master key for signing does not exist. false otherwise
     * If skipIfPresent = false, always returns true to continue with key rotation.
     */
    private fun shouldRotate(skipIfPresent: Boolean): Boolean {
        return if (skipIfPresent) {
            // TODO: Convert to exists query rather than a fetchOne query
            fetchForSigning() == null
        } else { true }
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
        EXPIRED("EXPIRED")
    }
}

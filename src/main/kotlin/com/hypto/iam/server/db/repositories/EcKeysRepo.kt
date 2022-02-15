package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.EcKeys
import com.hypto.iam.server.db.tables.records.EcKeysRecord
import com.hypto.iam.server.utils.MasterKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jooq.impl.DAOImpl

object EcKeysRepo : DAOImpl<EcKeysRecord, EcKeys, UUID>(
    com.hypto.iam.server.db.tables.EcKeys.EC_KEYS,
    EcKeys::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(ecKey: EcKeys): UUID {
        return ecKey.id
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchById(value: UUID): EcKeys? {
        return fetchOne(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.ID, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchById(value: String): EcKeys? {
        return fetchOne(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.ID, UUID.fromString(value))
    }

    /**
     * Fetch a unique record that has `status = "SIGNING"`
     */
    fun fetchForSigning(): EcKeys? {
        return fetchOne(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.STATUS, Status.SIGNING.value)
    }

    /**
     * Rotate master key in DB
     * @param oldKeyTtl TTL in seconds until which the rotated key must be available for verifying signatures.
     */
    // TODO: Move TTL to config file
    fun rotateKey(oldKeyTtl: Long = 600 /* 2X the local cache duration */): Boolean {

        MasterKey.generateKeyPair()

        ctx().transaction { c ->
            c.dsl().update(table).set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.STATUS, Status.EXPIRED.value)
                .where(
                    com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.STATUS.eq(Status.VERIFYING.value),
                    com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.UPDATED_AT.lessThan(
                        LocalDateTime.ofInstant(Instant.now().minusSeconds(oldKeyTtl), ZoneOffset.systemDefault())
                    )
                ).execute()

            c.dsl().update(table).set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.STATUS, Status.VERIFYING.value)
                .where(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.STATUS.eq(Status.SIGNING.value))
                .execute()

            c.dsl()
                .insertInto(table)
                .set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.STATUS, Status.SIGNING.value)
                .set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.PRIVATE_KEY, MasterKey.loadPrivateKeyDer())
                .set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.PUBLIC_KEY, MasterKey.loadPublicKeyDer())
                .set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.CREATED_AT, LocalDateTime.now())
                .set(com.hypto.iam.server.db.tables.EcKeys.EC_KEYS.UPDATED_AT, LocalDateTime.now())
                .execute()
        }
        return true
    }

    enum class Status(val value: String) {
        SIGNING("SIGNING"),
        VERIFYING("VERIFYING"),
        EXPIRED("EXPIRED")
    }
}

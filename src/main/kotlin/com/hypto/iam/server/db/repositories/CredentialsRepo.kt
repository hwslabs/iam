package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Credentials.CREDENTIALS
import com.hypto.iam.server.db.tables.pojos.Credentials
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.service.DatabaseFactory
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.util.UUID
import org.jooq.Result
import org.jooq.impl.DAOImpl

object CredentialsRepo : DAOImpl<CredentialsRecord, Credentials, UUID>(
    CREDENTIALS,
    Credentials::class.java,
    DatabaseFactory.getConfiguration()
) {
    override fun getId(credentials: Credentials): UUID {
        return credentials.id
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOneById(value: UUID): CredentialsRecord? {
        val cred = fetchOne(CREDENTIALS.ID, value)
        return cred?.let { CredentialsRecord(cred) }
    }

    /**
     * Fetch records that have `user_hrn = value`
     */
    fun fetchByUserHrn(value: String): Result<CredentialsRecord> {
        return ctx().selectFrom(table).where(CREDENTIALS.USER_HRN.eq(value)).fetch()
    }

    /**
     * Fetch records that have `user_hrn = value`
     */
    fun fetchByIdAndUserHrn(id: UUID, value: String): CredentialsRecord? {
        return ctx().selectFrom(table)
            .where(CREDENTIALS.ID.eq(id).and(CREDENTIALS.USER_HRN.eq(value)))
            .fetchOne()
    }

    /**
     * Fetch a unique record that has `refresh_token = value`
     */
    fun fetchByRefreshToken(refreshToken: String): CredentialsRecord? {
        return ctx().selectFrom(table).where(CREDENTIALS.REFRESH_TOKEN.eq(refreshToken)).fetchOne()
    }

    /**
     * Updates status and / or validUntil attributes of a credential represented by {@param id}
     * @return true on successful update. false otherwise.
     */
    fun update(id: UUID, status: Credential.Status?, validUntil: LocalDateTime?): CredentialsRecord? {
        var builder = ctx().update(table).set(CREDENTIALS.UPDATED_AT, LocalDateTime.now())
        status?.let { builder = builder.set(CREDENTIALS.STATUS, status.value) }
        validUntil?.let { builder = builder.set(CREDENTIALS.VALID_UNTIL, validUntil) }

        return builder.where(CREDENTIALS.ID.eq(id)).returning().fetchOne()
    }

    fun fetchAndUpdate(id: UUID, status: Credential.Status?, validUntil: LocalDateTime?): Boolean {
        val credentialsRecord = fetchOneById(id) ?: return false
        status?.let { credentialsRecord.setStatus(status.value) }
        validUntil?.let { credentialsRecord.setValidUntil(validUntil) }

        // TODO: Automate this using listeners or some other means
        if (credentialsRecord.changed()) { credentialsRecord.updatedAt = LocalDateTime.now() }

        return credentialsRecord.update() > 0
    }

    /**
     * Creates a credential record and returns the record object with auto-generated values (i.e, uuid)
     */
    fun create(
        userHrn: Hrn,
        status: Credential.Status = Credential.Status.active,
        refreshToken: String,
        validUntil: LocalDateTime? = null
    ): CredentialsRecord {
        val record = CredentialsRecord()
            .setValidUntil(validUntil).setStatus(status.value).setRefreshToken(refreshToken)
            .setUserHrn(userHrn.toString()).setCreatedAt(LocalDateTime.now()).setUpdatedAt(LocalDateTime.now())
        record.attach(configuration())
        record.store()
        return record
    }

    fun delete(organizationId: String, userId: String, id: UUID): Boolean {
        val record = CredentialsRecord()
            .setId(id)
            .setUserHrn(ResourceHrn(organizationId, "", IamResourceTypes.USER, userId).toString())
        record.attach(configuration())
        val count = record.delete()
        return count > 0
    }
}

package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Credentials.CREDENTIALS
import com.hypto.iam.server.db.tables.pojos.Credentials
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.util.UUID
import org.jooq.Result
import org.jooq.impl.DAOImpl

object CredentialsRepo : BaseRepo<CredentialsRecord, Credentials, UUID>() {

    private val idFun = fun (credentials: Credentials): UUID {
        return credentials.id
    }

    override suspend fun dao(): DAOImpl<CredentialsRecord, Credentials, UUID> {
        return txMan.getDao(CREDENTIALS, Credentials::class.java, idFun)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    suspend fun fetchOneById(value: UUID): CredentialsRecord? {
        val cred = fetchOne(CREDENTIALS.ID, value)
        return cred?.let { CredentialsRecord(cred) }
    }

    /**
     * Fetch records that have `user_hrn = value`
     */
    suspend fun fetchByUserHrn(value: String): Result<CredentialsRecord> {
        val dao = dao()
        return dao.ctx().selectFrom(dao.table).where(CREDENTIALS.USER_HRN.eq(value)).fetch()
    }

    /**
     * Fetch records that have `user_hrn = value`
     */
    suspend fun fetchByIdAndUserHrn(id: UUID, value: String): CredentialsRecord? {
        val dao = dao()
        return dao.ctx().selectFrom(dao.table)
            .where(CREDENTIALS.ID.eq(id).and(CREDENTIALS.USER_HRN.eq(value)))
            .fetchOne()
    }

    /**
     * Fetch a unique record that has `refresh_token = value`
     */
    suspend fun fetchByRefreshToken(refreshToken: String): CredentialsRecord? {
        val dao = dao()
        return dao.ctx().selectFrom(dao.table).where(CREDENTIALS.REFRESH_TOKEN.eq(refreshToken)).fetchOne()
    }

    /**
     * Updates status and / or validUntil attributes of a credential represented by {@param id}
     * @return true on successful update. false otherwise.
     */
    suspend fun update(id: UUID, status: Credential.Status?, validUntil: LocalDateTime?): CredentialsRecord? {
        val dao = dao()
        var builder = dao.ctx().update(dao.table).set(CREDENTIALS.UPDATED_AT, LocalDateTime.now())
        status?.let { builder = builder.set(CREDENTIALS.STATUS, status.value) }
        validUntil?.let { builder = builder.set(CREDENTIALS.VALID_UNTIL, validUntil) }

        return builder.where(CREDENTIALS.ID.eq(id)).returning().fetchOne()
    }

    suspend fun fetchAndUpdate(id: UUID, status: Credential.Status?, validUntil: LocalDateTime?): Boolean {
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
    suspend fun create(
        userHrn: Hrn,
        status: Credential.Status = Credential.Status.active,
        refreshToken: String,
        validUntil: LocalDateTime? = null
    ): CredentialsRecord {
        val record = CredentialsRecord()
            .setValidUntil(validUntil).setStatus(status.value).setRefreshToken(refreshToken)
            .setUserHrn(userHrn.toString()).setCreatedAt(LocalDateTime.now()).setUpdatedAt(LocalDateTime.now())
        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun delete(organizationId: String, userId: String, id: UUID): Boolean {
        val record = CredentialsRecord()
            .setId(id)
            .setUserHrn(ResourceHrn(organizationId, "", IamResources.USER, userId).toString())
        record.attach(dao().configuration())
        val count = record.delete()
        return count > 0
    }
}

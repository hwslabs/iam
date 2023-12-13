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

    private val idFun = fun(credentials: Credentials): UUID {
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
     * Fetch records that matches the `id` and `user_hrn`
     */
    suspend fun fetchByIdAndUserHrn(id: UUID, value: String): CredentialsRecord? {
        return ctx("credentials.fetch_by_id").selectFrom(CREDENTIALS)
            .where(CREDENTIALS.ID.eq(id).and(CREDENTIALS.USER_HRN.eq(value)))
            .fetchOne()
    }

    /**
     * Updates status to `inactive` for users with `userHrn`
     * @return true on successful update. false otherwise.
     */
    suspend fun updateStatusForUser(userHrn: String, status: Credential.Status = Credential.Status.inactive): Boolean {
        val count = ctx("credentials.update_status_for_user")
            .update(CREDENTIALS)
            .set(CREDENTIALS.STATUS, status.value)
            .set(CREDENTIALS.UPDATED_AT, LocalDateTime.now())
            .where(CREDENTIALS.USER_HRN.eq(userHrn))
            .execute()
        return count > 0
    }

    /**
     * Fetch a unique record that has `refresh_token = value`
     */
    suspend fun fetchByRefreshToken(refreshToken: String): CredentialsRecord? {
        return ctx("credentials.fetch_by_token")
            .selectFrom(CREDENTIALS).where(CREDENTIALS.REFRESH_TOKEN.eq(refreshToken)).fetchOne()
    }

    /**
     * Updates status and / or validUntil attributes of a credential represented by {@param id}
     * @return true on successful update. false otherwise.
     */
    suspend fun update(id: UUID, status: Credential.Status?, validUntil: LocalDateTime?): CredentialsRecord? {
        var builder = ctx("credentials.update").update(CREDENTIALS).set(CREDENTIALS.UPDATED_AT, LocalDateTime.now())
        status?.let { builder = builder.set(CREDENTIALS.STATUS, status.value) }
        validUntil?.let { builder = builder.set(CREDENTIALS.VALID_UNTIL, validUntil) }

        return builder.where(CREDENTIALS.ID.eq(id)).returning().fetchOne()
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

    suspend fun getAllByUserHrn(
        userHrn: Hrn
    ): Result<CredentialsRecord> {
        return ctx().selectFrom(CREDENTIALS)
            .where(CREDENTIALS.USER_HRN.eq(userHrn.toString()))
            .fetch()
    }

    suspend fun delete(organizationId: String, subOrganizationId: String?, userId: String, id: UUID): Boolean {
        val record = CredentialsRecord()
            .setId(id)
            .setUserHrn(ResourceHrn(organizationId, subOrganizationId, IamResources.USER, userId).toString())
        record.attach(dao().configuration())
        val count = record.delete()
        return count > 0
    }

    suspend fun deleteByUserHrn(userHrn: String): Boolean {
        val count = ctx("credentials.delete_by_user_hrn")
            .deleteFrom(CREDENTIALS)
            .where(CREDENTIALS.USER_HRN.like("%$userHrn%"))
            .execute()
        return count > 0
    }
}

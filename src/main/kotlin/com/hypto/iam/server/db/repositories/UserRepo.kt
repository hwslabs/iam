package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.Users.USERS
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.models.User
import com.hypto.iam.server.service.DatabaseFactory
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.util.Optional
import org.jooq.Result
import org.jooq.impl.DAOImpl

object UserRepo : DAOImpl<UsersRecord, Users, String>(
    USERS,
    Users::class.java, DatabaseFactory.getConfiguration()
) {

    override fun getId(user: Users): String {
        return user.hrn
    }

    /**
     * Fetch records that have `hrn = values`
     */
    fun fetchByHrn(hrn: String): UsersRecord? {
        return ctx()
            .selectFrom(table)
            .where(USERS.HRN.equal(hrn))
            .fetchOne()
    }

    /**
     * Fetch a unique record that has `hrn = value`
     */
    fun fetchOneById(value: String): Users? {
        return fetchOne(USERS.HRN, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOptionalById(value: String): Optional<Users> {
        return fetchOptional(USERS.HRN, value)
    }

    /**
     * Fetch records that have `organization_id IN (values)`
     */
    fun fetchByOrganizationId(vararg values: String): List<Users> {
        return fetch(USERS.ORGANIZATION_ID, *values)
    }

    fun fetchByOrganizationId(orgId: String): List<Users> {
        return ctx()
            .selectFrom(table)
            .where(USERS.ORGANIZATION_ID.eq(orgId))
            .fetch(mapper())
    }

    fun fetchByOrganizationIdPaginated(
        organizationId: String,
        paginationContext: PaginationContext
    ): Result<UsersRecord> {
        return ctx().selectFrom(table)
            .where(USERS.ORGANIZATION_ID.eq(organizationId))
            .paginate(USERS.HRN, paginationContext)
            .fetch()
    }

    @Suppress("LongParameterList")
    fun create(
        hrn: ResourceHrn,
        password: String,
        email: String,
        phone: String?,
        type: User.UserType,
        status: User.Status,
        createdByHrn: Hrn,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): UsersRecord {
        val record = UsersRecord()
            .setHrn(hrn.toString())
            .setPasswordHash(password)
            .setEmail(email)
            .setPhone(phone)
            .setLoginAccess(true)
            .setUserType(type.value)
            .setStatus(status.value)
            .setCreatedBy(createdByHrn.toString())
            .setOrganizationId(hrn.organization)
            .setCreatedAt(createdAt)
            .setUpdatedAt(updatedAt)

        record.attach(configuration())
        record.store()
        return record
    }
}

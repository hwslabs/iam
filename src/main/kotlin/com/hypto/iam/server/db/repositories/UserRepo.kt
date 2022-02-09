package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.service.DatabaseFactory
import java.util.Optional
import java.util.UUID
import org.jooq.impl.DAOImpl

object UserRepo : DAOImpl<UsersRecord, Users, UUID>(
    com.hypto.iam.server.db.tables.Users.USERS,
    Users::class.java, DatabaseFactory.getConfiguration()
) {

    override fun getId(user: Users): UUID? {
        return user.id
    }

    /**
     * Fetch records that have `id IN (values)`
     */
    fun fetchById(vararg values: UUID?): List<Users?> {
        return fetch(com.hypto.iam.server.db.tables.Users.USERS.ID, *values)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOneById(value: UUID?): Users? {
        return fetchOne(com.hypto.iam.server.db.tables.Users.USERS.ID, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOptionalById(value: UUID): Optional<Users?> {
        return fetchOptional(com.hypto.iam.server.db.tables.Users.USERS.ID, value)
    }

    /**
     * Fetch records that have `organization_id IN (values)`
     */
    fun fetchByOrganizationId(vararg values: String?): List<Users> {
        return fetch(com.hypto.iam.server.db.tables.Users.USERS.ORGANIZATION_ID, *values)
    }

    fun fetchByOrganizationId(orgId: String): List<Users> {
        return ctx()
            .selectFrom(table)
            .where(com.hypto.iam.server.db.tables.Users.USERS.ORGANIZATION_ID.eq(orgId))
            .fetch(mapper())
    }
}

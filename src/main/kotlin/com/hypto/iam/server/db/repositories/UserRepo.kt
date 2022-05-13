package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USERS
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.models.User
import java.time.LocalDateTime
import org.jooq.impl.DAOImpl

object UserRepo : BaseRepo<UsersRecord, Users, String>() {

    private val idFun = fun(user: Users): String {
        return user.hrn
    }

    override suspend fun dao(): DAOImpl<UsersRecord, Users, String> {
        return txMan.getDao(com.hypto.iam.server.db.tables.Users.USERS, Users::class.java, idFun)
    }

    suspend fun insert(user: Users) {
        dao().insert(user)
    }

    suspend fun existsByEmail(email: String, organizationId: String, uniqueCheck: Boolean = false): Boolean {
        val dao = dao()
        var builder = dao.ctx().selectFrom(dao.table)
            .where(USERS.EMAIL.eq(email))
            .and(USERS.DELETED.eq(false))

        builder = if (uniqueCheck) {
            // Check only verified users in other organizations and all users within the organization
            builder.and((USERS.VERIFIED.eq(true).andNot(USERS.ORGANIZATION_ID.eq(organizationId)))
                .or(USERS.ORGANIZATION_ID.eq(organizationId)))
        } else {
            // Check all verified and unverified users within the organization
            builder.and(USERS.ORGANIZATION_ID.eq(organizationId))
        }

        return dao.ctx().fetchExists(builder)
    }

    suspend fun update(hrn: String, status: User.Status?, verified: Boolean?): UsersRecord? {
        val dao = dao()
        val updateStep = dao.ctx().update(dao.table).set(USERS.UPDATED_AT, LocalDateTime.now())
        if (status != null) {
            updateStep.set(USERS.STATUS, status.value)
        }
        if (verified != null) {
            updateStep.set(USERS.VERIFIED, verified)
        }
        return updateStep.where(USERS.HRN.eq(hrn)).and(USERS.DELETED.eq(false))
            .returning().fetchOne()
    }

    suspend fun delete(hrn: String): UsersRecord? {
        val dao = dao()
        return dao.ctx().update(dao.table).set(USERS.UPDATED_AT, LocalDateTime.now())
            .set(USERS.DELETED, true)
            .where(USERS.HRN.eq(hrn))
            .returning().fetchOne()
    }
}

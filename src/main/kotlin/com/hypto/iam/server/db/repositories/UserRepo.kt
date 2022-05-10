package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USERS
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.UsersRecord
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
            builder.and((USERS.VERIFIED.eq(true).andNot(USERS.ORGANIZATION_ID.eq(organizationId)))
                .or(USERS.ORGANIZATION_ID.eq(organizationId)))
        } else {
            builder.and(USERS.ORGANIZATION_ID.eq(organizationId))
        }

        return dao.ctx().fetchExists(builder)
    }
}

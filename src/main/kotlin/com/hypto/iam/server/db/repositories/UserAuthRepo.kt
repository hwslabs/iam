package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USER_AUTH
import com.hypto.iam.server.db.tables.pojos.UserAuth
import com.hypto.iam.server.db.tables.records.UserAuthRecord
import com.hypto.iam.server.utils.Hrn
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record2
import org.jooq.impl.DAOImpl
import java.time.LocalDateTime

typealias UserAuthPk = Record2<String?, String?>

object UserAuthRepo : BaseRepo<UserAuthRecord, UserAuth, UserAuthPk>() {
    @Suppress("ktlint:standard:blank-line-before-declaration")
    private fun getIdFun(dsl: DSLContext): (UserAuth) -> UserAuthPk {
        return fun (userAuth: UserAuth): UserAuthPk {
            return dsl.newRecord(
                USER_AUTH.USER_HRN,
                USER_AUTH.PROVIDER_NAME,
            )
                .values(userAuth.userHrn, userAuth.providerName)
        }
    }

    override suspend fun dao(): DAOImpl<UserAuthRecord, UserAuth, UserAuthPk> {
        return txMan.getDao(
            USER_AUTH,
            UserAuth::class.java,
            getIdFun(txMan.dsl()),
        )
    }

    suspend fun fetchByUserHrnAndProviderName(
        hrn: String,
        providerName: String,
    ): UserAuthRecord? {
        return UserAuthRepo
            .ctx("userAuth.findByUserHrnAndProviderName")
            .selectFrom(USER_AUTH)
            .where(USER_AUTH.USER_HRN.eq(hrn).and(USER_AUTH.PROVIDER_NAME.eq(providerName)))
            .fetchOne()
    }

    suspend fun create(
        hrn: String,
        providerName: String,
        authMetadata: JSONB?,
    ): UserAuthRecord {
        val logTimestamp = LocalDateTime.now()
        val record =
            UserAuthRecord()
                .setUserHrn(hrn)
                .setProviderName(providerName)
                .setAuthMetadata(authMetadata)
                .setCreatedAt(logTimestamp)
                .setUpdatedAt(logTimestamp)

        record.attach(dao().configuration())
        record.store()
        return record
    }

    suspend fun fetchUserAuth(
        userHrn: Hrn,
    ): List<UserAuthRecord> {
        return ctx("userAuth.fetchUserAuth").selectFrom(USER_AUTH)
            .where(USER_AUTH.USER_HRN.eq(userHrn.toString()))
            .fetch()
    }

    suspend fun deleteByUserHrn(userHrn: String): Boolean {
        val count =
            ctx("userAuth.deleteByUserHrn")
                .deleteFrom(USER_AUTH)
                .where(USER_AUTH.USER_HRN.eq(userHrn))
                .execute()
        return count > 0
    }
}

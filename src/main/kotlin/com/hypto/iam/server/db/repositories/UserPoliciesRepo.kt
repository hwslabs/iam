package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.utils.Hrn
import java.util.UUID
import org.jooq.Result
import org.jooq.impl.DAOImpl

object UserPoliciesRepo : DAOImpl<UserPoliciesRecord, UserPolicies, UUID>(
    com.hypto.iam.server.db.tables.UserPolicies.USER_POLICIES,
    UserPolicies::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(userPolicies: UserPolicies): UUID {
        return userPolicies.id
    }

    /**
     * Fetch records that have `principal_hrn = value`
     */
    fun fetchByPrincipalHrn(value: String): Result<UserPoliciesRecord> {
        return ctx()
            .selectFrom(table)
            .where(USER_POLICIES.PRINCIPAL_HRN.equal(value))
            .fetch()
    }

    fun insert(records: List<UserPoliciesRecord>): Result<UserPoliciesRecord> {
        // No way to return generated values
        // https://github.com/jooq/jooq/issues/3327
        // return ctx().batchInsert(records).execute()

        var insertStep = ctx().insertInto(table)
        for (i in 0 until records.size - 1) {
            insertStep = insertStep.set(records[i]).newRecord()
        }
        val lastRecord = records[records.size - 1]

        return insertStep.set(lastRecord).returning().fetch()
    }

    fun delete(userHrn: Hrn, policies: List<String>): Boolean {
        val count = ctx().deleteFrom(table).where(
            USER_POLICIES.PRINCIPAL_HRN.eq(userHrn.toString()).and(USER_POLICIES.POLICY_HRN.`in`(policies))
        ).execute()

        return count > 0
    }
}

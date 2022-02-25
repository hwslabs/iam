package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.tables.Policies.POLICIES
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.customPaginate
import com.hypto.iam.server.extensions.paginate
import com.hypto.iam.server.utils.Hrn
import java.util.UUID
import org.jooq.Record
import org.jooq.Result
import org.jooq.TableField
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

    fun fetchByUserHrnPaginated(
        userHrn: String,
        paginationContext: PaginationContext
    ): Result<UserPoliciesRecord> {
        return ctx().selectFrom(table)
            .where(USER_POLICIES.PRINCIPAL_HRN.eq(userHrn))
            .paginate(USER_POLICIES.POLICY_HRN, paginationContext)
            .fetch()
    }

    fun fetchPoliciesByUserHrnPaginated(
        userHrn: String,
        paginationContext: PaginationContext
    ): Result<PoliciesRecord> {
        return customPaginate<Record, String>(
            ctx()
                .select(POLICIES.fields().asList())
                .from(
                    table.join(POLICIES).on(
                        com.hypto.iam.server.db.Tables.POLICIES.HRN.eq(USER_POLICIES.POLICY_HRN)
                    )
                )
                .where(USER_POLICIES.PRINCIPAL_HRN.eq(userHrn)),
            USER_POLICIES.POLICY_HRN as TableField<Record, String>,
            paginationContext
        ).fetchInto(POLICIES)
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

package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.tables.Policies.POLICIES
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.customPaginate
import com.hypto.iam.server.utils.Hrn
import java.util.UUID
import org.jooq.Record
import org.jooq.Result
import org.jooq.TableField
import org.jooq.impl.DAOImpl

object UserPoliciesRepo : BaseRepo<UserPoliciesRecord, UserPolicies, UUID>() {

    private val idFun = fun (userPolicies: UserPolicies): UUID = userPolicies.id

    override suspend fun dao(): DAOImpl<UserPoliciesRecord, UserPolicies, UUID> =
        txMan.getDao(USER_POLICIES, UserPolicies::class.java, idFun)

    /**
     * Fetch records that have `principal_hrn = value`
     */
    suspend fun fetchByPrincipalHrn(value: String): Result<UserPoliciesRecord> =
        ctx("userPolicies.fetchByPrincipalHrn")
            .selectFrom(USER_POLICIES)
            .where(USER_POLICIES.PRINCIPAL_HRN.equal(value))
            .fetch()

    suspend fun fetchPoliciesByUserHrnPaginated(
        userHrn: String,
        paginationContext: PaginationContext
    ): Result<PoliciesRecord> {
        return customPaginate<Record, String>(
            ctx("userPolicies.fetchByPrincipalHrnPaginated")
                .select(POLICIES.fields().asList())
                .from(
                    USER_POLICIES.join(POLICIES).on(
                        com.hypto.iam.server.db.Tables.POLICIES.HRN.eq(USER_POLICIES.POLICY_HRN)
                    )
                )
                .where(USER_POLICIES.PRINCIPAL_HRN.eq(userHrn)),
            USER_POLICIES.POLICY_HRN as TableField<Record, String>,
            paginationContext
        ).fetchInto(POLICIES)
    }

    suspend fun insert(records: List<UserPoliciesRecord>): Result<UserPoliciesRecord> {
        // No way to return generated values
        // https://github.com/jooq/jooq/issues/3327
        // return ctx().batchInsert(records).execute()

        var insertStep = ctx("userPolicies.insertBatch").insertInto(USER_POLICIES)
        for (i in 0 until records.size - 1) {
            insertStep = insertStep.set(records[i]).newRecord()
        }
        val lastRecord = records[records.size - 1]

        return insertStep.set(lastRecord).returning().fetch()
    }

    suspend fun delete(userHrn: Hrn, policies: List<String>): Boolean = ctx("userPolicies.delete")
        .deleteFrom(USER_POLICIES)
        .where(USER_POLICIES.PRINCIPAL_HRN.eq(userHrn.toString()), USER_POLICIES.POLICY_HRN.`in`(policies))
        .execute() > 0
}

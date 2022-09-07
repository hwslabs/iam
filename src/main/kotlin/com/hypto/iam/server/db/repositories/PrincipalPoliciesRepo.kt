package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.PRINCIPAL_POLICIES
import com.hypto.iam.server.db.tables.Policies.POLICIES
import com.hypto.iam.server.db.tables.pojos.PrincipalPolicies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.PrincipalPoliciesRecord
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.customPaginate
import com.hypto.iam.server.utils.Hrn
import java.util.UUID
import org.jooq.Record
import org.jooq.Result
import org.jooq.TableField
import org.jooq.impl.DAOImpl

object PrincipalPoliciesRepo : BaseRepo<PrincipalPoliciesRecord, PrincipalPolicies, UUID>() {

    private val idFun = fun (PrincipalPolicies: PrincipalPolicies): UUID = PrincipalPolicies.id

    override suspend fun dao(): DAOImpl<PrincipalPoliciesRecord, PrincipalPolicies, UUID> =
        txMan.getDao(PRINCIPAL_POLICIES, PrincipalPolicies::class.java, idFun)

    /**
     * Fetch records that have `principal_hrn = value`
     */
    suspend fun fetchByPrincipalHrn(value: String): Result<PrincipalPoliciesRecord> =
        ctx("principalPolicies.fetchByPrincipalHrn")
            .selectFrom(PRINCIPAL_POLICIES)
            .where(PRINCIPAL_POLICIES.PRINCIPAL_HRN.equal(value))
            .fetch()

    suspend fun fetchPoliciesByUserHrnPaginated(
        userHrn: String,
        paginationContext: PaginationContext
    ): Result<PoliciesRecord> {
        return customPaginate<Record, String>(
            ctx("principalPolicies.fetchByPrincipalHrnPaginated")
                .select(POLICIES.fields().asList())
                .from(
                    PRINCIPAL_POLICIES.join(POLICIES).on(
                        com.hypto.iam.server.db.Tables.POLICIES.HRN.eq(PRINCIPAL_POLICIES.POLICY_HRN)
                    )
                )
                .where(PRINCIPAL_POLICIES.PRINCIPAL_HRN.eq(userHrn)),
            PRINCIPAL_POLICIES.POLICY_HRN as TableField<Record, String>,
            paginationContext
        ).fetchInto(POLICIES)
    }

    suspend fun insert(records: List<PrincipalPoliciesRecord>): Result<PrincipalPoliciesRecord> {
        // No way to return generated values
        // https://github.com/jooq/jooq/issues/3327
        // return ctx().batchInsert(records).execute()

        var insertStep = ctx("principalPolicies.insertBatch").insertInto(PRINCIPAL_POLICIES)
        for (i in 0 until records.size - 1) {
            insertStep = insertStep.set(records[i]).newRecord()
        }
        val lastRecord = records[records.size - 1]

        return insertStep.set(lastRecord).returning().fetch()
    }

    suspend fun delete(userHrn: Hrn, policies: List<String>): Boolean = ctx("principalPolicies.delete")
        .deleteFrom(PRINCIPAL_POLICIES)
        .where(PRINCIPAL_POLICIES.PRINCIPAL_HRN.eq(userHrn.toString()), PRINCIPAL_POLICIES.POLICY_HRN.`in`(policies))
        .execute() > 0
}

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

object UserPoliciesRepo : BaseRepo<UserPoliciesRecord, UserPolicies, UUID>() {

    private val idFun = fun (userPolicies: UserPolicies): UUID {
        return userPolicies.id
    }

    override suspend fun dao(): DAOImpl<UserPoliciesRecord, UserPolicies, UUID> {
        return txMan.getDao(USER_POLICIES, UserPolicies::class.java, idFun)
    }

    /**
     * Fetch records that have `principal_hrn = value`
     */
    suspend fun fetchByPrincipalHrn(value: String): Result<UserPoliciesRecord> {
        val dao = dao()
        return dao.ctx()
            .selectFrom(dao.table)
            .where(USER_POLICIES.PRINCIPAL_HRN.equal(value))
            .fetch()
    }

    suspend fun fetchByUserHrnPaginated(
        userHrn: String,
        paginationContext: PaginationContext
    ): Result<UserPoliciesRecord> {
        val dao = dao()
        return dao.ctx().selectFrom(dao.table)
            .where(USER_POLICIES.PRINCIPAL_HRN.eq(userHrn))
            .paginate(USER_POLICIES.POLICY_HRN, paginationContext)
            .fetch()
    }

    suspend fun fetchPoliciesByUserHrnPaginated(
        userHrn: String,
        paginationContext: PaginationContext
    ): Result<PoliciesRecord> {
        val dao = dao()
        return customPaginate<Record, String>(
            dao.ctx()
                .select(POLICIES.fields().asList())
                .from(
                    dao.table.join(POLICIES).on(
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

        val dao = dao()
        var insertStep = dao.ctx().insertInto(dao.table)
        for (i in 0 until records.size - 1) {
            insertStep = insertStep.set(records[i]).newRecord()
        }
        val lastRecord = records[records.size - 1]

        return insertStep.set(lastRecord).returning().fetch()
    }

    suspend fun delete(userHrn: Hrn, policies: List<String>): Boolean {
        val dao = dao()
        val count = dao.ctx().deleteFrom(dao.table).where(
            USER_POLICIES.PRINCIPAL_HRN.eq(userHrn.toString()).and(USER_POLICIES.POLICY_HRN.`in`(policies))
        ).execute()

        return count > 0
    }
}

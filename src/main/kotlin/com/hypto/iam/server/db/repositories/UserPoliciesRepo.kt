package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.USER_POLICIES
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
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
}

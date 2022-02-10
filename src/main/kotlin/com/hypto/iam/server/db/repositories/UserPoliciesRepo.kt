package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import org.jooq.impl.DAOImpl
import java.util.Optional
import java.util.UUID

object UserPoliciesRepo : DAOImpl<UserPoliciesRecord, UserPolicies, UUID>(
    com.hypto.iam.server.db.tables.UserPolicies.USER_POLICIES,
    UserPolicies::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {
    override fun getId(userPolicies: UserPolicies): UUID {
        return userPolicies.id
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOneById(value: UUID): UserPolicies? {
        return fetchOne(com.hypto.iam.server.db.tables.UserPolicies.USER_POLICIES.ID, value)
    }

    /**
     * Fetch a unique record that has `id = value`
     */
    fun fetchOptionalById(value: UUID): Optional<UserPolicies> {
        return fetchOptional(com.hypto.iam.server.db.tables.UserPolicies.USER_POLICIES.ID, value)
    }

    /**
     * Fetch records that have `principal_hrn = value`
     */
    fun fetchByPrincipalHrn(value: String): List<UserPolicies> {
        return fetch(com.hypto.iam.server.db.tables.UserPolicies.USER_POLICIES.PRINCIPAL_HRN, value)
    }
}

package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import org.jooq.impl.DAOImpl

object PoliciesRepo : DAOImpl<PoliciesRecord, Policies, String>(
    com.hypto.iam.server.db.tables.Policies.POLICIES,
    Policies::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {

    override fun getId(policy: Policies): String {
        return policy.hrn
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(value: String): List<Policies> {
        return fetch(com.hypto.iam.server.db.tables.Policies.POLICIES.ORGANIZATION_ID, value)
    }

    /**
     * Fetch records that have `hrn = value`
     */
    fun fetchByHrn(hrn: String): Policies? {
        return fetchOne(com.hypto.iam.server.db.tables.Policies.POLICIES.HRN, hrn)
    }

    /**
     * Fetch records that have `organization_id = value, name IN (value, value, ...)`
     */
    fun fetchByHrns(hrns: List<String>): List<Policies> {
        return fetch(com.hypto.iam.server.db.tables.Policies.POLICIES.HRN, hrns)
    }
}

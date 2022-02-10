package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import org.jooq.Record2
import org.jooq.impl.DAOImpl

object PoliciesRepo : DAOImpl<PoliciesRecord, Policies, Record2<String, String>>(
    com.hypto.iam.server.db.tables.Policies.POLICIES,
    Policies::class.java,
    com.hypto.iam.server.service.DatabaseFactory.getConfiguration()
) {

    override fun getId(policy: Policies): Record2<String, String> {
        return compositeKeyRecord(policy.organizationId, policy.name)
    }

    /**
     * Fetch records that have `organization_id = value`
     */
    fun fetchByOrganizationId(value: String): List<Policies> {
        return fetch(com.hypto.iam.server.db.tables.Policies.POLICIES.ORGANIZATION_ID, value)
    }

    /**
     * Fetch records that have `organization_id = value AND name = value`
     */
    fun fetchByOrganizationIdAndName(organizationId: String, name: String): Policies? {
        return findById(
            ctx().newRecord(
                com.hypto.iam.server.db.tables.Policies.POLICIES.ORGANIZATION_ID,
                com.hypto.iam.server.db.tables.Policies.POLICIES.NAME
            ).values(organizationId, name)
        )
    }
}

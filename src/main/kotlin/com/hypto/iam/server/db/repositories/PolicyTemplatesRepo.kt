package com.hypto.iam.server.db.repositories

import com.hypto.iam.server.db.Tables.POLICY_TEMPLATES
import com.hypto.iam.server.db.tables.pojos.PolicyTemplates
import com.hypto.iam.server.db.tables.records.PolicyTemplatesRecord
import org.jooq.Result
import org.jooq.impl.DAOImpl

object PolicyTemplatesRepo : BaseRepo<PolicyTemplatesRecord, PolicyTemplates, String>() {

    enum class Status(val value: String) {
        ACTIVE("ACTIVE"),
        ARCHIVED("ARCHIVED");
    }

    private val idFun = fun (policyTemplates: PolicyTemplates) = policyTemplates.name
    override suspend fun dao(): DAOImpl<PolicyTemplatesRecord, PolicyTemplates, String> =
        txMan.getDao(POLICY_TEMPLATES, PolicyTemplates::class.java, idFun)

    /**
     * Fetch records that have `status = 'ACTIVE'`
     */
    suspend fun fetchActivePolicyTemplates(): Result<PolicyTemplatesRecord> = ctx("policy_templates.fetchActive")
        .selectFrom(POLICY_TEMPLATES)
        .where(POLICY_TEMPLATES.STATUS.eq(Status.ACTIVE.value))
        .fetch()
}

package com.hypto.iam.server.service

import com.hypto.iam.server.Constants.Companion.ORGANIZATION_ID_KEY
import com.hypto.iam.server.Constants.Companion.USER_HRN_KEY
import com.hypto.iam.server.db.repositories.PolicyTemplatesRepo
import com.hypto.iam.server.db.repositories.RawPolicyPayload
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.models.User
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import net.pwall.mustache.Template
import net.pwall.mustache.parser.MustacheParserException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PolicyTemplatesServiceImpl : KoinComponent, PolicyTemplatesService {
    private val policyTemplateRepo: PolicyTemplatesRepo by inject()
    private val policyService: PolicyService by inject()

    override suspend fun createPersistAndReturnRootPolicyRecordsForOrganization(
        organizationId: String,
        user: User,
    ): List<PoliciesRecord> {
        val templateVariablesMap = mapOf(ORGANIZATION_ID_KEY to organizationId, USER_HRN_KEY to user.hrn)
        val policyTemplates = policyTemplateRepo.fetchActivePolicyTemplates()
        val adminPolicyNames = policyTemplates.mapNotNullTo(mutableSetOf()) { if (it.isRootPolicy) it.name else null }

        val rawPolicyPayloadsList =
            policyTemplates.map {
                val template =
                    try {
                        Template.parse(it.statements)
                    } catch (e: MustacheParserException) {
                        throw IllegalStateException("Invalid template ${it.name} - ${e.localizedMessage}", e)
                    }
                RawPolicyPayload(
                    hrn = ResourceHrn(organizationId, "", IamResources.POLICY, it.name),
                    description = it.description,
                    statements = template.processToString(templateVariablesMap),
                )
            }

        val policies = policyService.batchCreatePolicyRaw(organizationId, rawPolicyPayloadsList)
        return policies.filter {
            adminPolicyNames.contains(ResourceHrn(it.hrn).resourceInstance)
        }
    }
}

interface PolicyTemplatesService {
    suspend fun createPersistAndReturnRootPolicyRecordsForOrganization(
        organizationId: String,
        user: User,
    ): List<PoliciesRecord>
}

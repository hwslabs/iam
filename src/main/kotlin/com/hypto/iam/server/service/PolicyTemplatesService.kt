package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.PolicyTemplatesRepo
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.utils.ResourceHrn
import net.pwall.mustache.Template
import net.pwall.mustache.parser.MustacheParserException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PolicyTemplatesServiceImpl : KoinComponent, PolicyTemplatesService {
    private val policyTemplateRepo: PolicyTemplatesRepo by inject()
    private val policyService: PolicyService by inject()
    private val organizationIdKey = "organization_id"
    override suspend fun createPersistAndReturnRootPolicyRecordsForOrganization(
        organizationId: String
    ): List<PoliciesRecord> {
        val templateVariablesMap = mapOf(organizationIdKey to organizationId)
        val policyTemplates = policyTemplateRepo.fetchActivePolicyTemplates()
        val adminPolicyNames = policyTemplates.mapNotNullTo(mutableSetOf()) { if (it.isRootPolicy) it.name else null }

        val rawPolicyPayloadsList = policyTemplates.map {
            val template = try {
                Template.parse(it.statements)
            } catch (e: MustacheParserException) {
                throw IllegalStateException("Invalid template ${it.name} - ${e.localizedMessage}", e)
            }

            it.name to template.processToString(templateVariablesMap)
        }

        val policies = policyService.batchCreatePolicyRaw(organizationId, rawPolicyPayloadsList)
        return policies.filter {
            adminPolicyNames.contains(ResourceHrn(it.hrn).resourceInstance)
        }
    }
}

interface PolicyTemplatesService {
    suspend fun createPersistAndReturnRootPolicyRecordsForOrganization(organizationId: String): List<PoliciesRecord>
}

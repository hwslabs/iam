package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.PolicyTemplatesRepo
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import net.pwall.mustache.Template
import net.pwall.mustache.parser.MustacheParserException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PolicyTemplatesServiceImpl : KoinComponent, PolicyTemplatesService {
    private val policyTemplateRepo: PolicyTemplatesRepo by inject()
    private val policyService: PolicyService by inject()
    private val organizationIdKey = "organization_id"
    override suspend fun createAndPersistPolicyRecordsForOrganization(organizationId: String): List<PoliciesRecord> {
        val templateVariablesMap = mapOf(organizationIdKey to organizationId)
        val rawPolicyPayloadsList = policyTemplateRepo.fetchActivePolicyTemplates().map {
            val template = try {
                Template.parse(it.statements)
            } catch (e: MustacheParserException) {
                throw IllegalStateException("Invalid template ${it.name} - ${e.localizedMessage}", e)
            }

            it.name to template.processToString(templateVariablesMap)
        }

        return policyService.batchCreatePolicyRaw(organizationId, rawPolicyPayloadsList)
    }
}

interface PolicyTemplatesService {
    suspend fun createAndPersistPolicyRecordsForOrganization(organizationId: String): List<PoliciesRecord>
}

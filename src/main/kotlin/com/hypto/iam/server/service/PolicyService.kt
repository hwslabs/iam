package com.hypto.iam.server.service

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.PrincipalPoliciesRepo
import com.hypto.iam.server.db.repositories.RawPolicyPayload
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.RESOURCE_NAME_REGEX
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyPaginatedResponse
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.utils.policy.PolicyBuilder
import io.ktor.server.plugins.BadRequestException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_grammar.html
class PolicyServiceImpl : KoinComponent, PolicyService {
    private val policyRepo: PoliciesRepo by inject()
    private val actionRepo: ActionRepo by inject()
    private val resourceRepo: ResourceRepo by inject()
    private val principalPolicyRepo: PrincipalPoliciesRepo by inject()
    private val appConfig: AppConfig by inject()

    override suspend fun createPolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyHrn = ResourceHrn(organizationId, "", IamResources.POLICY, name)
        if (policyRepo.existsById(policyHrn.toString())) {
            throw EntityAlreadyExistsException("Policy with name [$name] already exists")
        }

        val newPolicyBuilder = PolicyBuilder(policyHrn)
        statements.forEach { newPolicyBuilder.withStatement(it) }

        if (appConfig.app.strictPolicyStatementValidation) {
            validateStatements(organizationId, statements)
        }

        val policyRecord = policyRepo.create(policyHrn, newPolicyBuilder.build())
        return Policy.from(policyRecord)
    }

    /**
     * example of rawPolicyPayloadsList:
     *   [[policy_name_1, <policy_1_statements as string>], [policy_name_2, <policy_2_statements as string>]]
     */
    override suspend fun batchCreatePolicyRaw(
        organizationId: String,
        rawPolicyPayloadsList: List<Pair<String, String>>
    ): List<PoliciesRecord> {
        val policyHrnStrings = mutableListOf<String>()
        val rawPolicyPayloads = rawPolicyPayloadsList.map {
            val policyHrn = ResourceHrn(organizationId, "", IamResources.POLICY, it.first)
            policyHrnStrings.add(policyHrn.toString())
            RawPolicyPayload(policyHrn, it.second)
        }

        if (policyRepo.fetchByHrns(policyHrnStrings).isNotEmpty()) {
            throw EntityAlreadyExistsException("One or more policies already exists")
        }

        return policyRepo.batchCreate(rawPolicyPayloads)
    }

    override suspend fun getPolicy(organizationId: String, name: String): Policy {
        val policyRecord = policyRepo.fetchByHrn(
            ResourceHrn(organizationId, "", IamResources.POLICY, name).toString()
        ) ?: throw EntityNotFoundException("Policy not found")
        return Policy.from(policyRecord)
    }

    override suspend fun updatePolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyHrn = ResourceHrn(organizationId, "", IamResources.POLICY, name)
        val policyHrnStr = policyHrn.toString()

        val newPolicyBuilder = PolicyBuilder(policyHrn)
        statements.forEach { newPolicyBuilder.withStatement(it) }

        if (appConfig.app.strictPolicyStatementValidation) {
            validateStatements(organizationId, statements)
        }

        val policyRecord = policyRepo.update(
            policyHrnStr,
            newPolicyBuilder.build()
        )
        policyRecord ?: throw EntityNotFoundException("cannot find policy: $name")
        return Policy.from(policyRecord)
    }

    override suspend fun deletePolicy(organizationId: String, name: String): BaseSuccessResponse {
        val policyHrnStr = ResourceHrn(organizationId, "", IamResources.POLICY, name).toString()
        if (!policyRepo.deleteByHrn(policyHrnStr)) {
            throw EntityNotFoundException("Policy not found")
        }

        return BaseSuccessResponse(true)
    }

    override suspend fun getPoliciesByUser(
        organizationId: String,
        userId: String,
        context: PaginationContext
    ): PolicyPaginatedResponse {
        val policies = principalPolicyRepo
            .fetchPoliciesByUserHrnPaginated(
                ResourceHrn(organizationId, "", IamResources.USER, userId).toString(),
                context
            )
        val newContext = PaginationContext.from(policies.lastOrNull()?.hrn, context)
        return PolicyPaginatedResponse(policies.map { Policy.from(it) }, newContext.nextToken, newContext.toOptions())
    }

    override suspend fun listPolicies(organizationId: String, context: PaginationContext): PolicyPaginatedResponse {
        val policies = policyRepo.fetchByOrganizationIdPaginated(organizationId, context)
        val newContext = PaginationContext.from(policies.lastOrNull()?.hrn, context)
        return PolicyPaginatedResponse(policies.map { Policy.from(it) }, newContext.nextToken, newContext.toOptions())
    }

    @Suppress("ThrowsCount")
    private suspend fun validateStatements(organizationId: String, statements: List<PolicyStatement>) {
        val resourceIds = mutableListOf<String>()
        val actionIds = mutableListOf<String>()
        statements.forEach {
            val resourceId = fetchResourceIdFromStatementResource(it.resource)
            if (!resourceId.isNullOrEmpty()) {
                resourceIds.add(resourceId)
            }

            val actionId = fetchActionIdFromStatementAction(
                if (resourceId.isNullOrEmpty()) null else resourceId.substringAfterLast(":"),
                it.action
            )
            if (!actionId.isNullOrEmpty()) {
                actionIds.add(actionId)
            }
        }

        if (resourceIds.isNotEmpty()) {
            val existingResourceIds = resourceRepo.fetchResourcesFromHrns(organizationId, resourceIds).map {
                it.hrn
            }
            if (resourceIds.toSet().minus(existingResourceIds.toSet()).isNotEmpty()) {
                throw BadRequestException("1 or more Resources do not exist")
            }
        }
        if (actionIds.isNotEmpty()) {
            val existingActionIds = actionRepo.fetchActionsFromHrns(organizationId, actionIds).map {
                it.hrn
            }
            if (actionIds.toSet().minus(existingActionIds.toSet()).isNotEmpty()) {
                throw BadRequestException("1 or more Actions do not exist")
            }
        }
    }

    private fun fetchResourceIdFromStatementResource(resource: String): String? {
        if (resource.matches(ResourceHrn.RESOURCE_HRN_REGEX)) {
            val resourceHrn = ResourceHrn(resource)

            if (resourceHrn.resource != null && resourceHrn.resource.matches(RESOURCE_NAME_REGEX.toRegex())) {
                return resourceHrn.toString().substringBeforeLast("/")
            } else if (resourceHrn.resource != "*") {
                throw BadRequestException("Resource - $resource contains invalid resource name")
            }
        }

        return null
    }

    private fun fetchActionIdFromStatementAction(resource: String?, action: String): String? {
        if (action.matches(ActionHrn.ACTION_HRN_REGEX)) {
            val actionHrn = ActionHrn(action)

            if (!resource.isNullOrEmpty() && resource != actionHrn.resource) {
                throw BadRequestException("Action - $action does not belong to Resource - $resource")
            }

            if (actionHrn.action != null && actionHrn.action.matches(RESOURCE_NAME_REGEX.toRegex())) {
                return actionHrn.toString().substringBeforeLast("/")
            } else if (actionHrn.action != "*") {
                throw BadRequestException("Action - $action contains invalid action name")
            }
        }

        return null
    }
}

interface PolicyService {
    suspend fun createPolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy
    suspend fun getPolicy(organizationId: String, name: String): Policy
    suspend fun updatePolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy
    suspend fun deletePolicy(organizationId: String, name: String): BaseSuccessResponse
    suspend fun getPoliciesByUser(
        organizationId: String,
        userId: String,
        context: PaginationContext
    ): PolicyPaginatedResponse

    suspend fun listPolicies(organizationId: String, context: PaginationContext): PolicyPaginatedResponse
    suspend fun batchCreatePolicyRaw(
        organizationId: String,
        rawPolicyPayloadsList: List<Pair<String, String>>
    ): List<PoliciesRecord>
}

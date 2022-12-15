package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.ActionRepo
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.PrincipalPoliciesRepo
import com.hypto.iam.server.db.repositories.ResourceRepo
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
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

    override suspend fun createPolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyHrn = ResourceHrn(organizationId, "", IamResources.POLICY, name)
        if (policyRepo.existsById(policyHrn.toString())) {
            throw EntityAlreadyExistsException("Policy with name [$name] already exists")
        }

        val newPolicyBuilder = PolicyBuilder(policyHrn)
        statements.forEach { newPolicyBuilder.withStatement(it) }
        validateStatements(organizationId, statements)

        val policyRecord = policyRepo.create(policyHrn, newPolicyBuilder.build())
        return Policy.from(policyRecord)
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
        validateStatements(organizationId, statements)

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
                ResourceHrn(organizationId, "", IamResources.USER, userId).toString(), context
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
            var resourceHrn: ResourceHrn? = null
            if (it.resource.matches(ResourceHrn.RESOURCE_HRN_REGEX)) {
                resourceHrn = ResourceHrn(it.resource)
                if (resourceHrn.resource != "*" && resourceHrn.resource != null)
                    resourceIds.add(resourceHrn.toString().substringBeforeLast("/"))
            }
            if (it.action.matches(ActionHrn.ACTION_HRN_REGEX)) {
                val actionHrn = ActionHrn(it.action)
                if (resourceHrn != null && resourceHrn.resource != actionHrn.resource)
                    throw BadRequestException("Action - ${it.action} does not belong to Resource - ${it.resource}")
                if (actionHrn.action != "*" && actionHrn.action != null)
                    actionIds.add(actionHrn.toString().substringBeforeLast("/"))
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
}

package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyPaginatedResponse
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.policy.PolicyBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_grammar.html
class PolicyServiceImpl : KoinComponent, PolicyService {
    private val policyRepo: PoliciesRepo by inject()
    private val userPolicyRepo: UserPoliciesRepo by inject()
    private val gson: Gson by inject()

    override suspend fun createPolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyHrn = Hrn.of(organizationId, IamResourceTypes.POLICY, name)
        if (policyRepo.existsById(policyHrn.toString())) {
            throw EntityAlreadyExistsException("Policy with name [$name] already exists")
        }

        // TODO: Validate policy statements (actions and resourceTypes)
        val newPolicyBuilder = PolicyBuilder(policyHrn.toString())
        statements.forEach { newPolicyBuilder.withStatement(it) }

        val policyRecord = policyRepo.create(policyHrn, newPolicyBuilder.build())
        return Policy.from(policyRecord)
    }

    override suspend fun getPolicy(organizationId: String, name: String): Policy {
        val policyRecord = policyRepo.fetchByHrn(Hrn.of(organizationId, IamResourceTypes.POLICY, name).toString())
            ?: throw EntityNotFoundException("Policy not found")
        return Policy.from(policyRecord)
    }

    override suspend fun updatePolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyHrnStr = Hrn.of(organizationId, IamResourceTypes.POLICY, name).toString()

        // TODO: Validate policy statements (actions and resourceTypes)
        val newPolicyBuilder = PolicyBuilder(policyHrnStr)
        statements.forEach { newPolicyBuilder.withStatement(it) }

        val policyRecord = policyRepo.update(
            policyHrnStr,
            newPolicyBuilder.build()
        )
        policyRecord ?: throw IllegalStateException("Update unsuccessful")
        return Policy.from(policyRecord)
    }

    override suspend fun deletePolicy(organizationId: String, name: String): BaseSuccessResponse {
        if (!policyRepo.delete(organizationId, name)) { throw EntityNotFoundException("Policy not found") }

        return BaseSuccessResponse(true)
    }

    override suspend fun getPoliciesByUser(
        organizationId: String,
        userId: String,
        context: PaginationContext
    ): PolicyPaginatedResponse {
        val policies = userPolicyRepo
            .fetchPoliciesByUserHrnPaginated(Hrn.of(organizationId, IamResourceTypes.USER, userId).toString(), context)
        val newContext = PaginationContext.from(policies.lastOrNull()?.hrn, context)
        return PolicyPaginatedResponse(policies.map { Policy.from(it) }, newContext.nextToken, newContext.toOptions())
    }

    override suspend fun listPolicies(organizationId: String, context: PaginationContext): PolicyPaginatedResponse {
        val policies = policyRepo.fetchByOrganizationIdPaginated(organizationId, context)
        val newContext = PaginationContext.from(policies.lastOrNull()?.hrn, context)
        return PolicyPaginatedResponse(policies.map { Policy.from(it) }, newContext.nextToken, newContext.toOptions())
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

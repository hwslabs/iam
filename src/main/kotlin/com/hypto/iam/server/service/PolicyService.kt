package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.exceptions.EntityAlreadyExistsException
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.GetUserPoliciesResponse
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.UserPolicy
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_grammar.html
class PolicyServiceImpl : KoinComponent, PolicyService {
    private val policyRepo: PoliciesRepo by inject()
    private val userPolicyRepo: UserPoliciesRepo by inject()
    private val gson: Gson by inject()

    override fun createPolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyHrn = Hrn.of(organizationId, IamResourceTypes.POLICY, name)
        if (policyRepo.existsById(policyHrn.toString())) {
            throw EntityAlreadyExistsException("Policy with name [$name] already exists")
        }
        val policyRecord = policyRepo.create(policyHrn, gson.toJson(statements))
        return Policy.from(policyRecord)
    }

    override fun getPolicy(organizationId: String, name: String): Policy {
        val policyRecord = policyRepo.fetchByHrn(Hrn.of(organizationId, IamResourceTypes.POLICY, name).toString())
            ?: throw EntityNotFoundException("Policy not found")
        return Policy.from(policyRecord)
    }

    override fun updatePolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy {
        val policyRecord = policyRepo.update(
            Hrn.of(organizationId, IamResourceTypes.POLICY, name).toString(),
            gson.toJson(statements)
        )
        policyRecord ?: throw IllegalStateException("Update unsuccessful")
        return Policy.from(policyRecord)
    }

    override fun deletePolicy(organizationId: String, name: String): BaseSuccessResponse {
        if (!policyRepo.delete(organizationId, name)) { throw EntityNotFoundException("Policy not found") }

        return BaseSuccessResponse(true)
    }

    override fun getPoliciesByUser(organizationId: String, userId: String): GetUserPoliciesResponse {
        val userPolicies = userPolicyRepo
            .fetchByPrincipalHrn(Hrn.of(organizationId, IamResourceTypes.USER, userId).toString())
        return GetUserPoliciesResponse(userPolicies.map { UserPolicy.from(it) })
    }
}

interface PolicyService {
    fun createPolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy
    fun getPolicy(organizationId: String, name: String): Policy
    fun updatePolicy(organizationId: String, name: String, statements: List<PolicyStatement>): Policy
    fun deletePolicy(organizationId: String, name: String): BaseSuccessResponse
    fun getPoliciesByUser(organizationId: String, userId: String): GetUserPoliciesResponse
}

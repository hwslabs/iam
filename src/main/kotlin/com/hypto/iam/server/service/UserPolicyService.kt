package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.UserPoliciesRepo
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.policy.PolicyBuilder
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserPolicyServiceImpl : UserPolicyService, KoinComponent {
    private val policiesRepo: PoliciesRepo by inject()
    private val userPoliciesRepo: UserPoliciesRepo by inject()

    override suspend fun fetchEntitlements(userHrn: String): PolicyBuilder {
        val userPolicies = userPoliciesRepo.fetchByPrincipalHrn(userHrn)

        val policyBuilder = PolicyBuilder()
        userPolicies.forEach {
            val policy = policiesRepo.fetchByHrn(it.policyHrn)!!
            logger.info { policy.statements }

            policyBuilder.withPolicy(policy).withUserPolicy(it)
        }

        return policyBuilder
    }

    override suspend fun attachPoliciesToUser(principal: Hrn, policies: List<Hrn>): BaseSuccessResponse {
        if (!policiesRepo.existsByIds(policies.map { it.toString() })) {
            throw IllegalArgumentException("Invalid policies found")
        }

        userPoliciesRepo.insert(
            policies.map {
                UserPoliciesRecord()
                    .setPrincipalHrn(principal.toString())
                    .setPolicyHrn(it.toString())
                    .setCreatedAt(LocalDateTime.now())
            }
        )

        return BaseSuccessResponse(true)
    }

    override suspend fun detachPoliciesToUser(principal: Hrn, policies: List<Hrn>): BaseSuccessResponse {
        userPoliciesRepo.delete(principal, policies.map { it.toString() })
        return BaseSuccessResponse(true)
    }
}

interface UserPolicyService {
    suspend fun fetchEntitlements(userHrn: String): PolicyBuilder
    suspend fun attachPoliciesToUser(principal: Hrn, policies: List<Hrn>): BaseSuccessResponse
    suspend fun detachPoliciesToUser(principal: Hrn, policies: List<Hrn>): BaseSuccessResponse
}

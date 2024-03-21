package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.PoliciesRepo
import com.hypto.iam.server.db.repositories.PrincipalPoliciesRepo
import com.hypto.iam.server.db.tables.records.PrincipalPoliciesRecord
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.utils.policy.PolicyBuilder
import com.hypto.iam.server.utils.policy.PolicyVariables
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

class PrincipalPolicyServiceImpl : PrincipalPolicyService, KoinComponent {
    private val policiesRepo: PoliciesRepo by inject()
    private val principalPoliciesRepo: PrincipalPoliciesRepo by inject()

    override suspend fun fetchEntitlements(userHrn: String): PolicyBuilder {
        val principalPolicies = principalPoliciesRepo.fetchByPrincipalHrn(userHrn)
        val hrn = ResourceHrn(userHrn)
        val policyBuilder = PolicyBuilder().withPolicyVariables(PolicyVariables(organizationId = hrn.organization, userHrn = userHrn, userId = hrn.resourceInstance))
        principalPolicies.forEach {
            val policy = policiesRepo.fetchByHrn(it.policyHrn)!!
            logger.info { policy.statements }

            policyBuilder.withPolicy(policy).withPrincipalPolicy(it)
        }

        return policyBuilder
    }

    override suspend fun attachPoliciesToUser(
        principal: Hrn,
        policies: List<Hrn>,
    ): BaseSuccessResponse {
        require(policies.isNotEmpty()) {
            "No policy Hrns provided to attach"
        }

        require(policiesRepo.existsByIds(policies.map { it.toString() })) {
            "Invalid policies found"
        }

        principalPoliciesRepo.insert(
            policies.map {
                PrincipalPoliciesRecord()
                    .setPrincipalHrn(principal.toString())
                    .setPolicyHrn(it.toString())
                    .setCreatedAt(LocalDateTime.now())
            },
        )

        return BaseSuccessResponse(true)
    }

    override suspend fun detachPoliciesToUser(
        principal: Hrn,
        policies: List<Hrn>,
    ): BaseSuccessResponse {
        principalPoliciesRepo.delete(principal, policies.map { it.toString() })
        return BaseSuccessResponse(true)
    }
}

interface PrincipalPolicyService {
    suspend fun fetchEntitlements(userHrn: String): PolicyBuilder

    suspend fun attachPoliciesToUser(
        principal: Hrn,
        policies: List<Hrn>,
    ): BaseSuccessResponse

    suspend fun detachPoliciesToUser(
        principal: Hrn,
        policies: List<Hrn>,
    ): BaseSuccessResponse
}

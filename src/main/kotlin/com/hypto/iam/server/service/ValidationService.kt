package com.hypto.iam.server.service

import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.ResourceActionEffect
import com.hypto.iam.server.models.ResourceActionEffect.Effect
import com.hypto.iam.server.models.ValidationRequest
import com.hypto.iam.server.models.ValidationResponse
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.policy.PolicyRequest
import com.hypto.iam.server.utils.policy.PolicyValidator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ValidationServiceImpl : ValidationService, KoinComponent {
    private val principalPolicyService: PrincipalPolicyService by inject()
    private val policyValidator: PolicyValidator by inject()

    override suspend fun validateIfUserHasPermissionToActions(
        userHrn: Hrn,
        validationRequest: ValidationRequest
    ): ValidationResponse {
        val policyBuilder = principalPolicyService.fetchEntitlements(userHrn.toString())
        val validations = validationRequest.validations

        val results = policyValidator.batchValidate(
            policyBuilder,
            validations.map { PolicyRequest(userHrn.toString(), it.resource, it.action) }
        )

        return ValidationResponse(
            results.mapIndexed { i, it ->
                ResourceActionEffect.from(validations[i], if (it) { Effect.allow } else { Effect.deny })
            }
        )
    }
}

interface ValidationService {
    suspend fun validateIfUserHasPermissionToActions(
        userHrn: Hrn,
        validationRequest: ValidationRequest
    ): ValidationResponse
}

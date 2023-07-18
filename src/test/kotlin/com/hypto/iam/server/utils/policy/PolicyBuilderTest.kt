package com.hypto.iam.server.utils.policy

import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.PrincipalPoliciesRecord
import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PolicyBuilderTest : AbstractContainerBaseTest() {

    @Test
    fun `test policy builder - only statements`() {
        val orgId = "sampleOrgId"
        val resourceName1 = "sampleResourceName1"
        val resourceName2 = "sampleResourceName2"
        val policyHrn = ResourceHrn(orgId, "", IamResources.POLICY, "policy1")

        val (resourceHrn1, actionHrn1) = DataSetupHelper.generateResourceActionHrn(
            orgId,
            null,
            resourceName1,
            "action1"
        )
        val (resourceHrn2, actionHrn2) = DataSetupHelper.generateResourceActionHrn(
            orgId,
            null,
            resourceName2,
            "action2"
        )

        val builder = PolicyBuilder(policyHrn)
            .withStatement(
                PolicyStatement(
                    resourceHrn1,
                    actionHrn1,
                    PolicyStatement.Effect.allow
                )
            )
            .withStatement(
                PolicyStatement(
                    resourceHrn2,
                    actionHrn2,
                    PolicyStatement.Effect.deny
                )
            )
        val expectedPolicyText =
            "p, $policyHrn, $resourceHrn1, $actionHrn1, allow\np, $policyHrn, $resourceHrn2, $actionHrn2, deny\n"

        Assertions.assertEquals(expectedPolicyText, builder.build())
    }

    @Test
    fun `test policy builder - only policies`() {
        val orgId = "sampleOrgId"

        val policy1Hrn = ResourceHrn(orgId, "", IamResources.POLICY, "policy1")
        val policy1HrnStr = policy1Hrn.toString()

        val (resourceHrn1, actionHrn1) = DataSetupHelper.generateResourceActionHrn(
            orgId,
            null,
            "sampleResourceName1",
            "action1"
        )
        val (resourceHrn2, actionHrn2) = DataSetupHelper.generateResourceActionHrn(
            orgId,
            null,
            "sampleResourceName2",
            "action2"
        )
        val policy1Statements = PolicyBuilder(policy1Hrn)
            .withStatement(
                PolicyStatement(
                    resourceHrn1,
                    actionHrn1,
                    PolicyStatement.Effect.allow
                )
            )
            .withStatement(
                PolicyStatement(
                    resourceHrn2,
                    actionHrn2,
                    PolicyStatement.Effect.deny
                )
            )
            .toString()

        val policyRecord = PoliciesRecord(
            policy1HrnStr,
            orgId,
            1,
            policy1Statements,
            LocalDateTime.now(),
            LocalDateTime.now()
        )

        val userHrn = ResourceHrn(orgId, "", IamResources.USER, "user1").toString()
        val principalPoliciesRecord =
            PrincipalPoliciesRecord(UUID.randomUUID(), userHrn, policy1HrnStr, LocalDateTime.now())

        val expectedPolicyText =
            "p, $policy1Hrn, $resourceHrn1, $actionHrn1, allow\n" +
                "p, $policy1Hrn, $resourceHrn2, $actionHrn2, deny\n" +
                "\n" +
                "g, $userHrn, $policy1Hrn\n"

        val builder = PolicyBuilder()
            .withPolicy(policyRecord)
            .withPrincipalPolicy(principalPoliciesRecord)

        Assertions.assertEquals(expectedPolicyText, builder.build())
    }
}

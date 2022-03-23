package com.hypto.iam.server.utils

import com.hypto.iam.server.helpers.AbstractContainerBaseTest
import com.hypto.iam.server.helpers.DataSetupHelper
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.utils.policy.PolicyBuilder
import com.hypto.iam.server.utils.policy.PolicyRequest
import com.hypto.iam.server.utils.policy.PolicyValidator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PolicyValidatorTest : AbstractContainerBaseTest() {
    @Test
    fun `Test policy- allow and deny for same permission, outcome- deny`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data1", "read")
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.deny))
            .withStatement(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn))
        )
    }

    @Test
    fun `Test policy- allow for a permission and deny for *, outcome- deny`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val resourceHrn = ResourceHrn("orgId", "alice", "data1", null).toString()
        val anyResourceHrn = ResourceHrn("orgId", "alice", "*", null).toString()
        val actionHrn = ActionHrn("orgId", "alice", "data1", "read").toString()
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))
            .withStatement(PolicyStatement(anyResourceHrn, actionHrn, PolicyStatement.Effect.deny))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn))
        )
    }

    @Test
    fun `Test policy- deny for a permission and allow for *, outcome- deny`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val resourceHrn = ResourceHrn("orgId", "alice", "data1", null).toString()
        val anyResourceHrn = ResourceHrn("orgId", "alice", "*", null).toString()
        val actionHrn = ActionHrn("orgId", "alice", "*", "read").toString()
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.deny))
            .withStatement(PolicyStatement(anyResourceHrn, actionHrn, PolicyStatement.Effect.allow))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn))
        )
    }

    @Test
    fun `Test policy- no matching permission for principal, outcome- deny`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val policyHrn2 = ResourceHrn("orgId", "bob", IamResources.POLICY, "policy1").toString()

        val (resourceHrn1, actionHrn1) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data1", "read")
        val (resourceHrn2, actionHrn2) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data2", "read")
        val (resourceHrn3, actionHrn3) = DataSetupHelper.createResourceActionHrn("orgId", "bob", "data1", "read")
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn1, actionHrn1, PolicyStatement.Effect.deny))
            .withStatement(PolicyStatement(resourceHrn2, actionHrn2, PolicyStatement.Effect.allow))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn2, resourceHrn3, actionHrn3))
        )
    }

    @Test
    fun `Test policy- allow permission by exact match, outcome- allow`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()

        val (resourceHrn1, actionHrn1) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data1", "read")
        val (resourceHrn2, actionHrn2) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data2", "read")

        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn1, actionHrn1, PolicyStatement.Effect.deny))
            .withStatement(PolicyStatement(resourceHrn2, actionHrn2, PolicyStatement.Effect.allow))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn2, actionHrn2))
        )
    }

    @Test
    fun `Test policy- allow permission by regex match of resource`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val (anyResourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "*", "read")
        val (resourceHrn1, actionHrn1) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data1", "read")
        val (resourceHrn2, actionHrn2) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data2", "read")
        val (resourceHrn3, actionHrn3) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data3", "read")
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(anyResourceHrn, actionHrn, PolicyStatement.Effect.allow))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn1, actionHrn1))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn2, actionHrn2))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn3, actionHrn3))
        )
    }

    @Test
    fun `Test policy- allow permission by partial regex match of resource`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val (anyResourceInstanceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn(
            "orgId",
            "alice",
            "resource1",
            "read",
            "*"
        )

        val (resourceInstanceHrn1, actionHrn1) = DataSetupHelper.createResourceActionHrn(
            "orgId",
            "alice",
            "resource1",
            "read",
            "instance1"
        )

        val (resourceInstanceHrn2, actionHrn2) = DataSetupHelper.createResourceActionHrn(
            "orgId",
            "alice",
            "resource1",
            "read",
            "instance2"
        )

        val (resourceInstanceHrn3, actionHrn3) = DataSetupHelper.createResourceActionHrn(
            "orgId",
            "alice",
            "resource1",
            "read",
            "instance3"
        )

        val (resourceInstanceHrn4, actionHrn4) = DataSetupHelper.createResourceActionHrn(
            "orgId",
            "alice",
            "resource2",
            "read",
            "instance1"
        )

        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(anyResourceInstanceHrn, actionHrn, PolicyStatement.Effect.allow))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceInstanceHrn1, actionHrn1))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceInstanceHrn2, actionHrn2))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceInstanceHrn3, actionHrn3))
        )

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceInstanceHrn4, actionHrn4))
        )
    }

    @Test
    fun `Test policy- allow permission by regex match of action`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "*", "read")
        val actionHrn2 = ActionHrn("orgId", "alice", "data1", "write").toString()
        val actionHrn3 = ActionHrn("orgId", "alice", "data1", "update").toString()
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn2))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn3))
        )
    }

    @Test
    fun `Test policy- allow permission by partial regex match of action`() {
        val policyHrn = ResourceHrn("orgId", "alice", IamResources.POLICY, "policy1").toString()
        val (resourceHrn, actionHrn) = DataSetupHelper.createResourceActionHrn("orgId", "alice", "data1", "prefix*")
        val actionHrn1 = ActionHrn("orgId", "alice", "data1", "prefixread").toString()
        val actionHrn2 = ActionHrn("orgId", "alice", "data1", "prefixwrite").toString()
        val actionHrn3 = ActionHrn("orgId", "alice", "data1", "prefixupdate").toString()
        val actionHrn4 = ActionHrn("orgId", "alice", "data1", "read").toString()
        val policy = PolicyBuilder(policyHrn)
            .withStatement(PolicyStatement(resourceHrn, actionHrn, PolicyStatement.Effect.allow))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn1))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn2))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn3))
        )

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest(policyHrn, resourceHrn, actionHrn4))
        )
    }
}

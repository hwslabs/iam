package com.hypto.iam.server.utils

import com.hypto.iam.server.utils.policy.PolicyBuilder
import com.hypto.iam.server.utils.policy.PolicyRequest
import com.hypto.iam.server.utils.policy.PolicyStatement
import com.hypto.iam.server.utils.policy.PolicyValidator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PolicyValidatorTest {
    @Test
    fun `Test policy- allow and deny for same permission, outcome- deny`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "read", "deny"))
            .withStatement(PolicyStatement.p("alice", "data1", "read", "allow"))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "read"))
        )
    }

    @Test
    fun `Test policy- allow for a permission and deny for *, outcome- deny`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "read", "allow"))
            .withStatement(PolicyStatement.p("alice", "*", "read", "deny"))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "read"))
        )
    }

    @Test
    fun `Test policy- deny for a permission and allow for *, outcome- deny`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "read", "deny"))
            .withStatement(PolicyStatement.p("alice", "*", "read", "allow"))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "read"))
        )
    }

    @Test
    fun `Test policy- no matching permission for principal, outcome- deny`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "read", "deny"))
            .withStatement(PolicyStatement.p("alice", "data2", "read", "allow"))

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest("bob", "data1", "read"))
        )
    }

    @Test
    fun `Test policy- allow permission by exact match, outcome- allow`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "read", "deny"))
            .withStatement(PolicyStatement.p("alice", "data2", "read", "allow"))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data2", "read"))
        )
    }

    @Test
    fun `Test policy- allow permission by regex match of resource`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "*", "read", "allow"))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "read"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data2", "read"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data3", "read"))
        )
    }

    @Test
    fun `Test policy- allow permission by partial regex match of resource`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "resource1/*", "read", "allow"))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "resource1/instance1", "read"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "resource1/instance2", "read"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "resource1/instance3", "read"))
        )

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "resource2/instance1", "read"))
        )
    }

    @Test
    fun `Test policy- allow permission by regex match of action`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "*", "allow"))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "read"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "write"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "update"))
        )
    }

    @Test
    fun `Test policy- allow permission by partial regex match of action`() {
        val policy = PolicyBuilder()
            .withStatement(PolicyStatement.p("alice", "data1", "prefix*", "allow"))

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "prefixread"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "prefixwrite"))
        )

        Assertions.assertTrue(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "prefixupdate"))
        )

        Assertions.assertFalse(
            PolicyValidator.validate(policy.stream(), PolicyRequest("alice", "data1", "read")))
    }
}

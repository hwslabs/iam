package com.hypto.iam.server.utils.policy

import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PolicyBuilderTest {
    @Test
    fun `test policy builder - only statements`() {
        val builder = PolicyBuilder()
            .withStatement(PolicyStatement.p("policy1", "resource1", "action1", "allow"))
            .withStatement(PolicyStatement.p("policy1", "resource2", "action2", "deny"))
        val expectedPolicyText = "p, policy1, resource1, action1, allow\np, policy1, resource2, action2, deny\n"

        Assertions.assertEquals(expectedPolicyText, builder.build())
    }

    @Test
    fun `test policy builder - only policies`() {
        val policy1Hrn = "hws:iam:org1:policy/policy1"
        val policy1Statements = PolicyBuilder()
            .withStatement(PolicyStatement.p(policy1Hrn, "resource1", "action1", "allow"))
            .withStatement(PolicyStatement.p(policy1Hrn, "resource2", "action2", "deny"))
            .toString()

        val policyRecord = PoliciesRecord(
            policy1Hrn, "org1", 1, policy1Statements, LocalDateTime.now(), LocalDateTime.now()
        )

        val userHrn = "hws:iam:org1:user/user1"
        val userPoliciesRecord = UserPoliciesRecord(UUID.randomUUID(), userHrn, policy1Hrn, LocalDateTime.now())

        val expectedPolicyText =
            "p, $policy1Hrn, resource1, action1, allow\n" +
                "p, $policy1Hrn, resource2, action2, deny\n" +
                "\n" +
                "g, $userHrn, $policy1Hrn\n"

        val builder = PolicyBuilder()
            .withPolicy(policyRecord)
            .withUserPolicy(userPoliciesRecord)

        Assertions.assertEquals(expectedPolicyText, builder.build())
    }
}

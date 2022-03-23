package com.hypto.iam.server.utils.policy

import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.exceptions.IncorrectPolicyException
import com.hypto.iam.server.utils.HrnFactory
import java.io.InputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PolicyBuilder() : KoinComponent {
    val hrnFactory: HrnFactory by inject()

    constructor(policyName: String) : this() {
        this.policyName = policyName
        this.orgId = hrnFactory.getHrn(policyName).organization
    }

    lateinit var policyName: String
    lateinit var orgId: String
    var policyStatements = ArrayList<PolicyStatement>()
    var policies = ArrayList<PoliciesRecord>()
    var userPolicies = ArrayList<UserPoliciesRecord>()

    fun validateStatement(statement: com.hypto.iam.server.models.PolicyStatement) {
        val prefix = "hrn:${this.orgId}"

        if (!statement.resource.startsWith(prefix)) {
            throw IncorrectPolicyException("Organization id does not match for resource: ${statement.resource}")
        }
        if (!statement.resource.startsWith(prefix)) {
            throw IncorrectPolicyException("Organization id does not match for action: ${statement.action}")
        }
    }

    fun withStatement(
        statement: com.hypto.iam.server.models.PolicyStatement,
        principal: String = policyName
    ): PolicyBuilder {
        validateStatement(statement = statement)
        this.policyStatements.add(PolicyStatement.of(principal, statement))
        return this
    }

    fun withPolicy(policy: PoliciesRecord?): PolicyBuilder {
        if (policy != null) {
            this.policies.add(policy)
        }
        return this
    }

    fun withUserPolicy(userPolicy: UserPoliciesRecord?): PolicyBuilder {
        if (userPolicy != null) {
            this.userPolicies.add(userPolicy)
        }
        return this
    }

    fun build(): String {
        return toString()
    }

    fun stream(): InputStream {
        return build().byteInputStream()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        policyStatements.forEach { builder.appendLine(it.toString()) }
        policies.forEach { builder.appendLine(it.statements) }
        userPolicies.forEach { builder.appendLine(PolicyStatement.g(it.principalHrn, it.policyHrn)) }
        return builder.toString()
    }
}

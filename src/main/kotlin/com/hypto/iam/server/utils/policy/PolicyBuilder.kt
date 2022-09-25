package com.hypto.iam.server.utils.policy

import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.PrincipalPoliciesRecord
import com.hypto.iam.server.exceptions.PolicyFormatException
import com.hypto.iam.server.utils.ResourceHrn
import java.io.InputStream
import org.koin.core.component.KoinComponent

class PolicyBuilder() : KoinComponent {

    constructor(policyHrn: ResourceHrn) : this() {
        this.policyHrn = policyHrn
        this.orgId = policyHrn.organization
    }

    constructor(policyStr: String) : this() {
        this.policyString = policyStr
    }

    lateinit var policyHrn: ResourceHrn
    lateinit var orgId: String
    lateinit var policyString: String
    var policyStatements = ArrayList<PolicyStatement>()
    var policies = ArrayList<PoliciesRecord>()
    var principalPolicies = ArrayList<PrincipalPoliciesRecord>()

    private fun validateStatement(statement: com.hypto.iam.server.models.PolicyStatement) {
        val prefix = "hrn:${this.orgId}"

        if (!statement.resource.startsWith(prefix)) {
            throw PolicyFormatException("Organization id does not match for resource: ${statement.resource}")
        }
        if (!statement.resource.startsWith(prefix)) {
            throw PolicyFormatException("Organization id does not match for action: ${statement.action}")
        }
    }

    fun withStatement(
        statement: com.hypto.iam.server.models.PolicyStatement,
        principal: ResourceHrn = policyHrn
    ): PolicyBuilder {
        validateStatement(statement = statement)
        this.policyStatements.add(PolicyStatement.of(principal.toString(), statement))
        return this
    }

    fun withPolicy(policy: PoliciesRecord?): PolicyBuilder {
        if (policy != null) {
            this.policies.add(policy)
        }
        return this
    }

    fun withPrincipalPolicy(principalPolicy: PrincipalPoliciesRecord?): PolicyBuilder {
        if (principalPolicy != null) {
            this.principalPolicies.add(principalPolicy)
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
        if (this::policyString.isInitialized) { return this.policyString }
        val builder = StringBuilder()
        policyStatements.forEach { builder.appendLine(it.toString()) }
        policies.forEach { builder.appendLine(it.statements) }
        principalPolicies.forEach { builder.appendLine(PolicyStatement.g(it.principalHrn, it.policyHrn)) }
        return builder.toString()
    }
}

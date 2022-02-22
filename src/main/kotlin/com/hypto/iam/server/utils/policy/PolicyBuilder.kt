package com.hypto.iam.server.utils.policy

import com.hypto.iam.server.db.tables.records.PoliciesRecord
import java.io.InputStream

class PolicyBuilder {

    var policyStatements = ArrayList<PolicyStatement>()
    var policies = ArrayList<PoliciesRecord>()

    fun withStatement(statement: PolicyStatement): PolicyBuilder {
        this.policyStatements.add(statement)
        return this
    }

    fun withPolicy(policy: PoliciesRecord?): PolicyBuilder {
        if (policy != null) { this.policies.add(policy) }
        return this
    }

    fun build(): String { return toString() }

    fun stream(): InputStream {
        return build().byteInputStream()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        policyStatements.forEach { builder.appendLine(it.statement) }
        policies.forEach { builder.appendLine(it.statements) }
        return builder.toString()
    }
}

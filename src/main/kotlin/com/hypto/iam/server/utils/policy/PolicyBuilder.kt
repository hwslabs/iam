package com.hypto.iam.server.utils.policy

import java.io.InputStream

class PolicyBuilder {

    var policyStatements = ArrayList<PolicyStatement>()

    fun withStatement(statement: PolicyStatement): PolicyBuilder {
        this.policyStatements.add(statement)
        return this
    }

    fun build(): String {
        val builder = StringBuilder()
        policyStatements.forEach { builder.appendLine(it.statement) }
        return builder.toString()
    }

    fun stream(): InputStream {
        return build().byteInputStream()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        policyStatements.forEach { builder.appendLine(it.statement) }
        return builder.toString()
    }
}

package com.hypto.iam.server.utils

import org.casbin.jcasbin.main.Enforcer
import org.casbin.jcasbin.persist.file_adapter.FileAdapter
import java.io.InputStream

object PolicyUtil {
    private val modelPath = this::class.java.classLoader.getResource("casbin_model.conf")?.path

    fun validate(policyBuilder: PolicyBuilder, policyRequest: PolicyRequest): Boolean {
        return validate(policyBuilder.stream(), policyRequest)
    }

    fun validate(inputStream: InputStream, policyRequest: PolicyRequest): Boolean {
        return Enforcer(modelPath, FileAdapter(inputStream))
            .enforce(policyRequest.principal, policyRequest.resource, policyRequest.action)
    }
}

data class PolicyRequest(val principal: String, val resource: String, val action: String)

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

class PolicyStatement(val statement: String) {
    companion object {
        fun p(principal: String, resource: String, action: String, effect: String): PolicyStatement {
            return PolicyStatement("p, $principal, $resource, $action, $effect")
        }

        fun g(principal: String, policy: String): PolicyStatement {
            return PolicyStatement("g, $principal, $policy")
        }
    }
}

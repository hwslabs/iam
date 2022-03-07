@file:Suppress("MagicNumber")

package com.hypto.iam.server.utils.policy

open class PolicyStatement(private val statement: String) {

    companion object {
        fun p(principal: String, resource: String, action: String, effect: String): PolicyStatement {
            return PolicyStatement("p, $principal, $resource, $action, $effect")
        }

        fun g(principal: String, policy: String): PolicyStatement {
            return PolicyStatement("g, $principal, $policy")
        }

        fun of(principal: String, statement: com.hypto.iam.server.models.PolicyStatement): PolicyStatement {
            return this.p(principal, statement.resource, statement.action, statement.effect.value)
        }
    }

    override fun toString(): String {
        return statement
    }
}

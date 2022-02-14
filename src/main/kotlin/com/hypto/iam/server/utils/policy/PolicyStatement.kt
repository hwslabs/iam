package com.hypto.iam.server.utils.policy

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

package com.hypto.iam.server.utils.policy

data class PolicyRequest(val principal: String, val resource: String, val action: String)

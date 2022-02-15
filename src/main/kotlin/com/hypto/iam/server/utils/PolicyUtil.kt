package com.hypto.iam.server.utils

import com.hypto.iam.server.utils.policy.PolicyBuilder
import com.hypto.iam.server.utils.policy.PolicyRequest
import java.io.InputStream
import org.casbin.jcasbin.main.Enforcer
import org.casbin.jcasbin.persist.file_adapter.FileAdapter

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

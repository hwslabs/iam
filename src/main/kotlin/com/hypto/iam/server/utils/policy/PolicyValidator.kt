package com.hypto.iam.server.utils.policy

import java.io.InputStream
import org.casbin.jcasbin.main.CoreEnforcer.newModel
import org.casbin.jcasbin.main.Enforcer
import org.casbin.jcasbin.persist.file_adapter.FileAdapter

object PolicyValidator {
    private val modelPath = this::class.java.classLoader.getResource("casbin_model.conf")?.path
    private val model = newModel(modelPath, "")

    fun validate(policyBuilder: PolicyBuilder, policyRequest: PolicyRequest): Boolean {
        return validate(policyBuilder.stream(), policyRequest)
    }

    fun validate(inputStream: InputStream, policyRequest: PolicyRequest): Boolean {
        return validate(Enforcer(model, FileAdapter(inputStream)), policyRequest)
    }

    fun validate(enforcer: Enforcer, policyRequest: PolicyRequest): Boolean {
        return enforcer
            .enforce(policyRequest.principal, policyRequest.resource, policyRequest.action)
    }

    fun batchValidate(policyBuilder: PolicyBuilder, policyRequests: List<PolicyRequest>): List<Boolean> {
        val enforcer = Enforcer(model, FileAdapter(policyBuilder.stream()))
        return policyRequests.map { validate(enforcer, it) }
    }
}

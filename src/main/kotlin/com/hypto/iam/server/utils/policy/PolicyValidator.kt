package com.hypto.iam.server.utils.policy

import java.io.InputStream
import org.casbin.jcasbin.main.CoreEnforcer.newModel
import org.casbin.jcasbin.main.Enforcer
import org.casbin.jcasbin.persist.file_adapter.FileAdapter

object PolicyValidator {
    private val modelStream = this::class.java.getResourceAsStream("/casbin_model.conf")
    private val model = modelStream?.let { newModel(String(it.readAllBytes(), Charsets.UTF_8)) }

    init {
        requireNotNull(modelStream) { "casbin_model.conf not found in resources" }
    }

    fun validate(policyBuilder: PolicyBuilder, policyRequest: PolicyRequest): Boolean {
        return validate(policyBuilder.stream(), policyRequest)
    }

    fun validate(inputStream: InputStream, policyRequest: PolicyRequest): Boolean {
        return validate(inputStream, listOf(policyRequest))
    }

    fun validate(inputStream: InputStream, policyRequests: List<PolicyRequest>): Boolean {
        return policyRequests
            .all {
                Enforcer(model, FileAdapter(inputStream))
                    .enforce(it.principal, it.resource, it.action)
            }
    }

    fun validate(enforcer: Enforcer, policyRequest: PolicyRequest): Boolean {
        return enforcer
            .enforce(policyRequest.principal, policyRequest.resource, policyRequest.action)
    }

    fun validateAny(inputStream: InputStream, policyRequests: List<PolicyRequest>): Boolean {
        return policyRequests
            .any {
                Enforcer(model, FileAdapter(inputStream))
                    .enforce(it.principal, it.resource, it.action)
            }
    }

    fun validateNone(inputStream: InputStream, policyRequests: List<PolicyRequest>): Boolean {
        return policyRequests
            .none {
                Enforcer(model, FileAdapter(inputStream))
                    .enforce(it.principal, it.resource, it.action)
            }
    }

    fun batchValidate(policyBuilder: PolicyBuilder, policyRequests: List<PolicyRequest>): List<Boolean> {
        val enforcer = Enforcer(model, FileAdapter(policyBuilder.stream()))
        return policyRequests.map { validate(enforcer, it) }.toList()
    }
}

package com.hypto.iam.server.utils.policy

import java.io.InputStream
import org.casbin.jcasbin.main.CoreEnforcer.newModel
import org.casbin.jcasbin.main.Enforcer
import org.casbin.jcasbin.persist.file_adapter.FileAdapter

object PolicyValidator {
    private val modelPath = this::class.java.classLoader.getResource("casbin_model.conf")?.path
    private val model = newModel(modelPath, "")

    // TODO: Get the policy from DB
    val samplePolicy = PolicyBuilder()
        .withStatement(
            PolicyStatement.p("hrn:hypto::iam-user/alice",
                "hrn:hypto:swiggy:ledger-ledgerAccount/12345",
                "hrn:hypto\$ledger-ledgerAccount:addTransactions",
                "allow"))
        .withStatement(
            PolicyStatement.p("hrn:hypto:swiggy:iam-user/bob",
                "hrn:hypto\$iam-resource",
                "hrn:hypto\$iam-resource:createAction",
                "allow"))

    fun validate(policyBuilder: PolicyBuilder, policyRequest: PolicyRequest): Boolean {
        return validate(policyBuilder.stream(), policyRequest)
    }

    fun validate(inputStream: InputStream, policyRequest: PolicyRequest): Boolean {
        return validate(inputStream, listOf(policyRequest))
    }

    fun validate(inputStream: InputStream, policyRequests: List<PolicyRequest>): Boolean {
        return policyRequests
            .all {
                Enforcer(modelPath, FileAdapter(inputStream))
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
                Enforcer(modelPath, FileAdapter(inputStream))
                    .enforce(it.principal, it.resource, it.action)
            }
    }

    fun validateNone(inputStream: InputStream, policyRequests: List<PolicyRequest>): Boolean {
        return policyRequests
            .none {
                Enforcer(modelPath, FileAdapter(inputStream))
                    .enforce(it.principal, it.resource, it.action)
            }
    }

    fun batchValidate(policyBuilder: PolicyBuilder, policyRequests: List<PolicyRequest>): List<Boolean> {
        val enforcer = Enforcer(model, FileAdapter(policyBuilder.stream()))
        return policyRequests.map { validate(enforcer, it) }.toList()
    }
}

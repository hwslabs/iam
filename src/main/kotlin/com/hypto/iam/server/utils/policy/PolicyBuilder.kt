package com.hypto.iam.server.utils.policy

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.PrincipalPoliciesRecord
import com.hypto.iam.server.exceptions.PolicyFormatException
import com.hypto.iam.server.utils.ResourceHrn
import net.pwall.mustache.Template
import net.pwall.mustache.parser.MustacheParserException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.InputStream

val mapTypeToken = object : TypeToken<Map<String, String?>>() {}

fun String.toSnakeCase(): String = PropertyNamingStrategies.SnakeCaseStrategy().translate(this)

class PolicyVariables(
    val organizationId: String,
    val userHrn: String? = null,
    val userId: String? = null,
)

class PolicyBuilder : KoinComponent {
    private val gson: Gson by inject()
    constructor()

    constructor(policyHrn: ResourceHrn) {
        this.policyHrn = policyHrn
        this.orgId = policyHrn.organization
    }

    constructor(policyStr: String) {
        this.policyString = policyStr
    }

    lateinit var policyHrn: ResourceHrn
    lateinit var orgId: String
    lateinit var policyString: String
    lateinit var policyVariables: PolicyVariables
    var policyStatements = ArrayList<PolicyStatement>()
    var policies = ArrayList<PoliciesRecord>()
    var principalPolicies = ArrayList<PrincipalPoliciesRecord>()

    private fun validateStatement(statement: com.hypto.iam.server.models.PolicyStatement) {
        val prefix = "hrn:${this.orgId}"
        val templatePrefix = "hrn:{{organizationId}}"

        if (!statement.resource.startsWith(prefix) && !statement.resource.startsWith(templatePrefix)) {
            throw PolicyFormatException("Organization id does not match for resource: ${statement.resource}")
        }
        if (!statement.action.startsWith(prefix) && !statement.action.startsWith(templatePrefix)) {
            throw PolicyFormatException("Organization id does not match for action: ${statement.action}")
        }
    }

    fun withStatement(
        statement: com.hypto.iam.server.models.PolicyStatement,
        principal: ResourceHrn = policyHrn,
    ): PolicyBuilder {
        validateStatement(statement = statement)
        this.policyStatements.add(PolicyStatement.of(principal.toString(), statement))
        return this
    }

    fun withPolicy(policy: PoliciesRecord?): PolicyBuilder {
        if (policy != null) {
            this.policies.add(policy)
        }
        return this
    }

    fun withPrincipalPolicy(principalPolicy: PrincipalPoliciesRecord?): PolicyBuilder {
        if (principalPolicy != null) {
            this.principalPolicies.add(principalPolicy)
        }
        return this
    }

    fun withPolicyVariables(policyVariables: PolicyVariables): PolicyBuilder {
        this.policyVariables = policyVariables
        return this
    }

    @Suppress("SwallowedException")
    fun build(): String {
        if (this::policyString.isInitialized) {
            return this.policyString
        }
        val builder = StringBuilder()
        policyStatements.forEach { builder.appendLine(it.toString()) }
        policies.forEach { builder.appendLine(it.statements) }
        principalPolicies.forEach { builder.appendLine(PolicyStatement.g(it.principalHrn, it.policyHrn)) }

        val policy = builder.toString()
        return if (this::policyVariables.isInitialized) {
            val template =
                try {
                    Template.parse(policy)
                } catch (e: MustacheParserException) {
                    throw IllegalStateException("Invalid policy format: $policy")
                }
            val variablesMap = gson.fromJson(gson.toJson(policyVariables), mapTypeToken)
            template.processToString(variablesMap.mapKeys { it.key.toSnakeCase() })
        } else {
            policy
        }
    }

    fun stream(): InputStream {
        return build().byteInputStream()
    }

    override fun toString(): String {
        return build()
    }
}

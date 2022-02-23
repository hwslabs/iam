package com.hypto.iam.server.extensions

import com.hypto.iam.server.db.tables.pojos.Credentials
import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.CredentialWithoutSecret
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.UserPolicy
import com.hypto.iam.server.utils.Hrn
import java.time.format.DateTimeFormatter

fun Credential.Companion.from(record: CredentialsRecord): Credential {
    return Credential(
        record.id.toString(),
        Credential.Status.valueOf(record.status),
        record.refreshToken,
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun Credential.Companion.from(record: Credentials): Credential {
    return Credential(
        record.id.toString(),
        Credential.Status.valueOf(record.status),
        record.refreshToken,
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun CredentialWithoutSecret.Companion.from(record: CredentialsRecord): CredentialWithoutSecret {
    return CredentialWithoutSecret(
        record.id.toString(),
        CredentialWithoutSecret.Status.valueOf(record.status),
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun CredentialWithoutSecret.Companion.from(record: Credentials): CredentialWithoutSecret {
    return CredentialWithoutSecret(
        record.id.toString(),
        CredentialWithoutSecret.Status.valueOf(record.status),
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

// // Inject Gson into Policy model
// val Policy.Companion.gson: Gson
//    get() = getKoinInstance()

val COMMA_REGEX = Regex("\\s*,\\s*")
fun PolicyStatement.Companion.from(policyString: String): PolicyStatement {
    val components = policyString.trim().split(COMMA_REGEX)
    if (components.size != 5 && components[0] != "p") {
        println(components)
        println(components.size)
        println(components[0])
        throw IllegalArgumentException("Invalid statement string")
    }
    return PolicyStatement(components[2], components[3], PolicyStatement.Effect.valueOf(components[4]))
}

fun Policy.Companion.from(record: PoliciesRecord): Policy {
    val hrn = Hrn.of(record.hrn)
    return Policy(
        hrn.resourceInstance!!,
        hrn.organization,
        record.version,
        record.statements.trim().lines().map { PolicyStatement.from(it) }
    )
}

fun Policy.Companion.from(record: Policies): Policy {
    val hrn = Hrn.of(record.hrn)
    return Policy(
        hrn.resourceInstance!!,
        hrn.organization,
        record.version,
        record.statements.split("\n").map { PolicyStatement.from(it) }
    )
}

fun UserPolicy.Companion.from(record: UserPoliciesRecord): UserPolicy {
    val policyHrn = Hrn.of(record.policyHrn)
    return UserPolicy(
        policyHrn.resourceInstance!!,
        policyHrn.organization
    )
}

fun UserPolicy.Companion.from(record: UserPolicies): UserPolicy {
    val policyHrn = Hrn.of(record.policyHrn)
    return UserPolicy(
        policyHrn.resourceInstance!!,
        policyHrn.organization
    )
}

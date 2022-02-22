package com.hypto.iam.server.extensions

import com.google.gson.Gson
import com.hypto.iam.server.db.tables.pojos.Credentials
import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.di.getKoinInstance
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

// Inject Gson into Policy model
val Policy.Companion.gson: Gson
    get() = getKoinInstance()

fun Policy.Companion.from(record: PoliciesRecord): Policy {
    val hrn = Hrn.of(record.hrn)
    return Policy(
        hrn.resourceInstance!!,
        hrn.organization,
        record.version,
        // TODO: Find a way to convert directly to list and avoid intermediate array
        gson.fromJson(record.statements, Array<PolicyStatement>::class.java).asList()
    )
}

fun Policy.Companion.from(record: Policies): Policy {
    val hrn = Hrn.of(record.hrn)
    return Policy(
        hrn.resourceInstance!!,
        hrn.organization,
        record.version,

        // TODO: Find a way to convert directly to list and avoid intermediate array
        gson.fromJson(record.statements, Array<PolicyStatement>::class.java).asList()
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

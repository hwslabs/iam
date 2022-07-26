package com.hypto.iam.server.extensions

import com.hypto.iam.server.db.tables.pojos.Actions
import com.hypto.iam.server.db.tables.pojos.AuditEntries
import com.hypto.iam.server.db.tables.pojos.Credentials
import com.hypto.iam.server.db.tables.pojos.Policies
import com.hypto.iam.server.db.tables.pojos.Resources
import com.hypto.iam.server.db.tables.pojos.UserPolicies
import com.hypto.iam.server.db.tables.pojos.Users
import com.hypto.iam.server.db.tables.records.ActionsRecord
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.db.tables.records.PoliciesRecord
import com.hypto.iam.server.db.tables.records.ResourcesRecord
import com.hypto.iam.server.db.tables.records.UserPoliciesRecord
import com.hypto.iam.server.db.tables.records.UsersRecord
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.models.Action
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.CredentialWithoutSecret
import com.hypto.iam.server.models.Policy
import com.hypto.iam.server.models.PolicyStatement
import com.hypto.iam.server.models.Resource
import com.hypto.iam.server.models.ResourceAction
import com.hypto.iam.server.models.ResourceActionEffect
import com.hypto.iam.server.models.UpdateUserRequest
import com.hypto.iam.server.models.User
import com.hypto.iam.server.models.UserPolicy
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.HrnFactory
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val hrnFactory = getKoinInstance<HrnFactory>()

fun Credential.Companion.from(record: CredentialsRecord): Credential {
    return Credential(
        record.id.toString(),
        Credential.Status.valueOf(record.status),
        record.refreshToken,
        record.createdAt.toUTCOffset(),
        record.updatedAt.toUTCOffset(),
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun Credential.Companion.from(record: Credentials): Credential {
    return Credential(
        record.id.toString(),
        Credential.Status.valueOf(record.status),
        record.refreshToken,
        record.createdAt.toUTCOffset(),
        record.updatedAt.toUTCOffset(),
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun CredentialWithoutSecret.Companion.from(record: CredentialsRecord): CredentialWithoutSecret {
    return CredentialWithoutSecret(
        record.id.toString(),
        CredentialWithoutSecret.Status.valueOf(record.status),
        record.createdAt.toUTCOffset(),
        record.updatedAt.toUTCOffset(),
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun CredentialWithoutSecret.Companion.from(record: Credentials): CredentialWithoutSecret {
    return CredentialWithoutSecret(
        record.id.toString(),
        CredentialWithoutSecret.Status.valueOf(record.status),
        record.createdAt.toUTCOffset(),
        record.updatedAt.toUTCOffset(),
        record.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

object MagicNumber {
    const val ONE = 1
    const val ZERO = 0
    const val TWO = 2
    const val THREE = 3
    const val FOUR = 4
    const val FIVE = 5
}

val COMMA_REGEX = Regex("\\s*,\\s*")
fun PolicyStatement.Companion.from(policyString: String): PolicyStatement {
    val components = policyString.trim().split(COMMA_REGEX)
    if (components.size != MagicNumber.FIVE && components[MagicNumber.ZERO] != "p") {
        throw IllegalArgumentException("Invalid statement string")
    }
    return PolicyStatement(
        components[MagicNumber.TWO],
        components[MagicNumber.THREE],
        PolicyStatement.Effect.valueOf(components[MagicNumber.FOUR])
    )
}

fun Policy.Companion.from(record: PoliciesRecord): Policy {
    val hrn = hrnFactory.getHrn(record.hrn)
    require(hrn is ResourceHrn) { "Hrn should be an instance of resourceHrn" }
    return Policy(
        name = hrn.resourceInstance!!,
        organizationId = hrn.organization,
        version = record.version,
        hrn = hrn.toString(),
        statements = record.statements.trim().lines().map { PolicyStatement.from(it) }
    )
}

fun Policy.Companion.from(record: Policies): Policy {
    val hrn = hrnFactory.getHrn(record.hrn)
    require(hrn is ResourceHrn) { "Hrn should be an instance of resourceHrn" }
    return Policy(
        name = hrn.resourceInstance!!,
        organizationId = hrn.organization,
        version = record.version,
        hrn = hrn.toString(),
        statements = record.statements.split("\n").map { PolicyStatement.from(it) }
    )
}

fun Resource.Companion.from(record: ResourcesRecord): Resource {
    val hrn = hrnFactory.getHrn(record.hrn)
    require(hrn is ResourceHrn) { "Hrn should be an instance of ResourceHrn" }
    return Resource(
        hrn.resource!!,
        hrn.organization,
        hrn.toString(),
        record.description
    )
}

fun Resource.Companion.from(record: Resources): Resource {
    val hrn = hrnFactory.getHrn(record.hrn)
    require(hrn is ResourceHrn) { "Hrn should be an instance of ResourceHrn" }
    return Resource(
        hrn.resource!!,
        hrn.organization,
        hrn.toString(),
        record.description
    )
}

fun Action.Companion.from(record: ActionsRecord): Action {
    val hrn = hrnFactory.getHrn(record.hrn)
    require(hrn is ActionHrn) { "Hrn should be an instance of ActionHrn" }
    return Action(
        hrn.organization,
        hrn.resource!!,
        hrn.action!!,
        hrn.toString(),
        record.description
    )
}

fun Action.Companion.from(record: Actions): Action {
    val hrn = hrnFactory.getHrn(record.hrn)
    require(hrn is ActionHrn) { "Hrn should be an instance of ActionHrn" }
    return Action(
        hrn.organization,
        hrn.resource!!,
        hrn.action!!,
        hrn.toString(),
        record.description
    )
}

fun User.Companion.from(value: UsersRecord): User {
    val hrn = hrnFactory.getHrn(value.hrn) as ResourceHrn
    return User(
        value.hrn, hrn.resourceInstance!!, value.organizationId,
        value.email, User.Status.valueOf(value.status),
        value.verified
    )
}

fun usersFrom(value: UsersRecord): Users {
    return Users(
        value.hrn, value.email, value.status,
        value.organizationId, value.createdAt, value.updatedAt,
        value.verified, value.deleted
    )
}

fun UserPolicy.Companion.from(record: UserPoliciesRecord): UserPolicy {
    val policyHrn = hrnFactory.getHrn(record.policyHrn)
    require(policyHrn is ResourceHrn) { "Hrn should be an instance of resourceHrn" }
    return UserPolicy(
        policyHrn.resourceInstance!!,
        policyHrn.organization
    )
}

fun UserPolicy.Companion.from(record: UserPolicies): UserPolicy {
    val policyHrn = hrnFactory.getHrn(record.policyHrn)
    require(policyHrn is ResourceHrn) { "Hrn should be an instance of resourceHrn" }
    return UserPolicy(
        policyHrn.resourceInstance!!,
        policyHrn.organization
    )
}

fun ResourceActionEffect.Companion.from(
    resourceAction: ResourceAction,
    effect: ResourceActionEffect.Effect
): ResourceActionEffect {
    return ResourceActionEffect(resourceAction.resource, resourceAction.action, effect)
}

fun auditEntryFrom(
    requestId: String?,
    eventTime: LocalDateTime,
    principal: String,
    resource: String,
    operation: String
): AuditEntries {
    val principalHrn: ResourceHrn = hrnFactory.getHrn(principal) as ResourceHrn
    return AuditEntries(null, requestId, eventTime, principalHrn.organization, principal, resource, operation, null)
}

fun UpdateUserRequest.Status.toUserStatus() = when (this) {
    UpdateUserRequest.Status.enabled -> User.Status.enabled
    UpdateUserRequest.Status.disabled -> User.Status.disabled
}

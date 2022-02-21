package com.hypto.iam.server.extensions

import com.hypto.iam.server.db.tables.pojos.Credentials
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.CredentialWithoutSecret
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

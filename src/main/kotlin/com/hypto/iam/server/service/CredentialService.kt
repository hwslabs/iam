package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.CredentialWithoutSecret
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.security.auditLog
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CredentialServiceImpl : KoinComponent, CredentialService {
    private val repo: CredentialsRepo by inject()
    private val idGenerator: ApplicationIdUtil.Generator by inject()

    override suspend fun createCredential(
        organizationId: String,
        userId: String,
        validUntil: String?
    ): Credential {
        val userHrn = ResourceHrn(organizationId, "", IamResourceTypes.USER, userId)
        // TODO: Limit number of active credentials for a single user
        val credentialsRecord = repo.create(
            userHrn = userHrn,
            refreshToken = idGenerator.refreshToken(organizationId),
            validUntil = validUntil?.let { LocalDateTime.parse(validUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        )

        // TODO: remove it after resolving error
//        auditLog().append(userHrn)

        return Credential.from(credentialsRecord)
    }

    override suspend fun getCredentialWithoutSecret(
        organizationId: String,
        userId: String,
        id: UUID
    ): CredentialWithoutSecret {
        val userHrn = ResourceHrn(organizationId, "", IamResourceTypes.USER, userId)
        return CredentialWithoutSecret.from(fetchCredential(userHrn, id))
    }

    override suspend fun updateCredentialAndGetWithoutSecret(
        organizationId: String,
        userId: String,
        id: UUID,
        status: UpdateCredentialRequest.Status?,
        validUntil: String?
    ): CredentialWithoutSecret {
        val userHrn = ResourceHrn(organizationId, "", IamResourceTypes.USER, userId)
        // TODO: 3 DB queries are being made in this method. Try to optimize
        fetchCredential(userHrn, id) // Validation

        val credentialsRecord = repo.update(
            id,
            status?.let { Credential.Status.valueOf(it.value) },
            validUntil?.let { LocalDateTime.parse(validUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        )

        credentialsRecord ?: throw IllegalStateException("Update unsuccessful")

        auditLog().append(userHrn)

        return CredentialWithoutSecret.from(credentialsRecord)
    }

    override suspend fun deleteCredential(organizationId: String, userId: String, id: UUID): BaseSuccessResponse {
        if (!repo.delete(organizationId, userId, id)) {
            throw EntityNotFoundException("Credential not found")
        }

        return BaseSuccessResponse(true)
    }

    private fun fetchCredential(userHrn: Hrn, id: UUID): CredentialsRecord {
        return repo.fetchByIdAndUserHrn(id, userHrn.toString())
            ?: throw EntityNotFoundException("Credential not found")
    }
}

interface CredentialService {
    suspend fun createCredential(organizationId: String, userId: String, validUntil: String? = null): Credential
    suspend fun getCredentialWithoutSecret(organizationId: String, userId: String, id: UUID): CredentialWithoutSecret
    suspend fun updateCredentialAndGetWithoutSecret(
        organizationId: String,
        userId: String,
        id: UUID,
        status: UpdateCredentialRequest.Status?,
        validUntil: String?
    ): CredentialWithoutSecret

    suspend fun deleteCredential(organizationId: String, userId: String, id: UUID): BaseSuccessResponse
}

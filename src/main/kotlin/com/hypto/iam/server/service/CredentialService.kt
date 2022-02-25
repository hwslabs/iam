package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.CredentialWithoutSecret
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.utils.ApplicationIdUtil
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.ResourceHrn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
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
        // TODO: Limit number of active credentials for a single user
        val credentialsRecord = repo.create(
            userHrn = ResourceHrn(organizationId, "", IamResourceTypes.USER, userId),
            refreshToken = idGenerator.refreshToken(organizationId),
            validUntil = LocalDateTime.parse(validUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        return Credential.from(credentialsRecord)
    }

    override suspend fun getCredentialWithoutSecret(
        organizationId: String,
        userId: String,
        id: UUID
    ): CredentialWithoutSecret {
        return CredentialWithoutSecret.from(fetchCredential(organizationId, userId, id))
    }

    override suspend fun updateCredentialAndGetWithoutSecret(
        organizationId: String,
        userId: String,
        id: UUID,
        status: UpdateCredentialRequest.Status?,
        validUntil: String?
    ): CredentialWithoutSecret {
        // TODO: 3 DB queries are being made in this method. Try to optimize
        fetchCredential(organizationId, userId, id) // Validation

        val credentialsRecord = repo.update(
            id,
            status?.let { Credential.Status.valueOf(it.value) },
            validUntil?.let { LocalDateTime.parse(validUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        )

        credentialsRecord ?: throw IllegalStateException("Update unsuccessful")

        return CredentialWithoutSecret.from(credentialsRecord)
    }

    override suspend fun deleteCredential(organizationId: String, userId: String, id: UUID): BaseSuccessResponse {
        if (!repo.delete(organizationId, userId, id)) { throw EntityNotFoundException("Credential not found") }

        return BaseSuccessResponse(true)
    }

    private fun fetchCredential(organizationId: String, userId: String, id: UUID): CredentialsRecord {
        return repo.fetchByIdAndUserHrn(id, ResourceHrn(organizationId, "", IamResourceTypes.USER, userId).toString())
            ?: throw EntityNotFoundException("Credential not found")
    }
}

interface CredentialService {
    suspend fun createCredential(organizationId: String, userId: String, validUntil: String?): Credential
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

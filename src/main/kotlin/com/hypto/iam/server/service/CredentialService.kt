package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.db.tables.records.CredentialsRecord
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.Credential
import com.hypto.iam.server.models.CredentialWithoutSecret
import com.hypto.iam.server.models.UpdateCredentialRequest
import com.hypto.iam.server.utils.Hrn
import com.hypto.iam.server.utils.IamResourceTypes
import com.hypto.iam.server.utils.IdUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CredentialServiceImpl : KoinComponent, CredentialService {
    private val repo: CredentialsRepo by inject()
    private val idUtil: IdUtil by inject()

    companion object {
        const val REFRESH_TOKEN_RANDOM_LENGTH = 30L
    }

    // First 10 chars: alphabets (lower + upper) representing userId
    // next 20 chars: alphanumeric with upper and lower case - random
    private fun generateRefreshToken(userId: String): String {
        return userId + idUtil.timeBasedRandomId(REFRESH_TOKEN_RANDOM_LENGTH, IdUtil.IdCharset.ALPHABETS)
    }

    override suspend fun createCredential(
        organizationId: String,
        userId: String,
        validUntil: String?
    ): Credential {
        val credentialsRecord = repo.create(
            userHrn = Hrn.of(organizationId, IamResourceTypes.USER, userId),
            refreshToken = generateRefreshToken(userId),
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
        fetchCredential(organizationId, userId, id) // Validation

        val success = repo.update(
            id,
            status?.let { Credential.Status.valueOf(it.value) },
            validUntil?.let { LocalDateTime.parse(validUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        )

        if (!success) { throw IllegalStateException("Update unsuccessful") }

        return CredentialWithoutSecret.from(repo.fetchOneById(id)!!)
    }

    override suspend fun deleteCredential(organizationId: String, userId: String, id: UUID): BaseSuccessResponse {
        if (!repo.delete(organizationId, userId, id)) { throw IllegalStateException("Credential not found") }

        return BaseSuccessResponse(true)
    }

    private fun fetchCredential(organizationId: String, userId: String, id: UUID): CredentialsRecord {
        return repo.fetchByIdAndUserHrn(id, Hrn.of(organizationId, IamResourceTypes.USER, userId).toString())
            ?: throw IllegalStateException("Credential not found")
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

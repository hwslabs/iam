package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.AuthProviderRepo
import com.hypto.iam.server.extensions.PaginationContext
import com.hypto.iam.server.extensions.from
import com.hypto.iam.server.models.AuthProvider
import com.hypto.iam.server.models.AuthProviderPaginatedResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthProviderServiceImpl : KoinComponent, AuthProviderService {
    private val authProviderRepo: AuthProviderRepo by inject()

    override suspend fun listAuthProvider(context: PaginationContext): AuthProviderPaginatedResponse {
        val authProviders = authProviderRepo.fetchAuthProvidersPaginated(context)
        val newContext = PaginationContext.from(authProviders.lastOrNull()?.createdAt.toString(), context)
        return AuthProviderPaginatedResponse(
            authProviders.map { AuthProvider.from(it) },
            newContext.nextToken,
            newContext.toOptions()
        )
    }
}

interface AuthProviderService {
    suspend fun listAuthProvider(
        context: PaginationContext
    ): AuthProviderPaginatedResponse
}

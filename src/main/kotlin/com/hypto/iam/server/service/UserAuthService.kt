package com.hypto.iam.server.service

import com.hypto.iam.server.authProviders.AuthProviderRegistry
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.repositories.UserAuthRepo
import com.hypto.iam.server.db.repositories.UserRepo
import com.hypto.iam.server.exceptions.EntityNotFoundException
import com.hypto.iam.server.models.BaseSuccessResponse
import com.hypto.iam.server.models.UserAuthMethod
import com.hypto.iam.server.models.UserAuthMethodsResponse
import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.AuthenticationException
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserAuthServiceImpl : KoinComponent, UserAuthService {
    private val organizationRepo: OrganizationRepo by inject()
    private val userRepo: UserRepo by inject()
    private val userAuthRepo: UserAuthRepo by inject()
    override suspend fun createUserAuth(
        organizationId: String,
        userId: String,
        issuer: String,
        token: String
    ): BaseSuccessResponse {
        if (issuer != TokenServiceImpl.ISSUER) {
            val authProvider = AuthProviderRegistry.getProvider(issuer) ?: throw AuthenticationException(
                "Invalid issuer"
            )
            val oAuthUserPrincipal = authProvider.getProfileDetails(TokenCredential(token, TokenType.OAUTH))
            val user = userRepo.findByEmail(oAuthUserPrincipal.email) ?: throw AuthenticationException(
                "User has not signed up yet"
            )
            val userAuth = userAuthRepo.fetchByUserHrnAndProviderName(user.hrn, issuer)
            if (userAuth != null) {
                userAuthRepo.create(
                    user.hrn,
                    oAuthUserPrincipal.issuer,
                    oAuthUserPrincipal.metadata?.let { AuthMetadata.toJsonB(it) }
                )
            } else {
                throw AuthenticationException("User already has this auth method")
            }
        }
        return BaseSuccessResponse(true)
    }

    override suspend fun listUserAuth(
        organizationId: String,
        userId: String
    ): UserAuthMethodsResponse {
        organizationRepo.findById(organizationId)
            ?: throw EntityNotFoundException("Invalid organization id name. Unable to get user")
        val userAuth = userAuthRepo.fetchUserAuth(
            ResourceHrn(organizationId, "", IamResources.USER, userId)
        ).map {
            UserAuthMethod(
                providerName = it.providerName
            )
        }.toList()

        return UserAuthMethodsResponse(userAuth)
    }
}

interface UserAuthService {
    suspend fun createUserAuth(
        organizationId: String,
        userId: String,
        issuer: String,
        token: String
    ): BaseSuccessResponse
    suspend fun listUserAuth(
        organizationId: String,
        userHrn: String
    ): UserAuthMethodsResponse
}

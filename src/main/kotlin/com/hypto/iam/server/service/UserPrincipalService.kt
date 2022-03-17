package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.exceptions.InternalException
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserPrincipalServiceImpl : KoinComponent, UserPrincipalService {
    private val credentialsRepo: CredentialsRepo by inject()
    private val userPoliciesService: UserPolicyService by inject()
    private val tokenService: TokenService by inject()

    override suspend fun getUserPrincipalByRefreshToken(tokenCredential: TokenCredential): UserPrincipal? {
        return credentialsRepo.fetchByRefreshToken(tokenCredential.value!!)?.let { credential ->
            UserPrincipal(
                tokenCredential,
                credential.userHrn,
                userPoliciesService.fetchEntitlements(credential.userHrn)
            )
        }
    }

    override suspend fun getUserPrincipalByJwtToken(tokenCredential: TokenCredential): UserPrincipal {
        if (tokenCredential.type != TokenType.JWT || tokenCredential.value == null)
            throw InternalException("Invalid token credential")
        val token = tokenService.validateJwtToken(tokenCredential.value)
        val userHrnStr: String = token.body.get(TokenServiceImpl.USER_CLAIM, String::class.java)
        return UserPrincipal(
            tokenCredential,
            userHrnStr,
            userPoliciesService.fetchEntitlements(userHrnStr)
        )
    }
}

interface UserPrincipalService {
    suspend fun getUserPrincipalByRefreshToken(tokenCredential: TokenCredential): UserPrincipal?
    suspend fun getUserPrincipalByJwtToken(tokenCredential: TokenCredential): UserPrincipal?
}

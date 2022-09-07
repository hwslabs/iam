package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.UsernamePasswordCredential
import com.hypto.iam.server.validators.validate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserPrincipalServiceImpl : KoinComponent, UserPrincipalService {
    private val credentialsRepo: CredentialsRepo by inject()
    private val principalPolicyService: PrincipalPolicyService by inject()
    private val tokenService: TokenService by inject()
    private val usersService: UsersService by inject()

    override suspend fun getUserPrincipalByRefreshToken(tokenCredential: TokenCredential): UserPrincipal? {
        return credentialsRepo.fetchByRefreshToken(tokenCredential.value!!)?.let { credential ->
            UserPrincipal(
                tokenCredential,
                credential.userHrn,
                principalPolicyService.fetchEntitlements(credential.userHrn)
            )
        }
    }

    override suspend fun getUserPrincipalByJwtToken(tokenCredential: TokenCredential): UserPrincipal {
        val token = tokenService.validateJwtToken(tokenCredential.value!!)
        val userHrnStr: String = token.body.get(TokenServiceImpl.USER_CLAIM, String::class.java)
        return UserPrincipal(
            tokenCredential,
            userHrnStr,
            principalPolicyService.fetchEntitlements(userHrnStr)
        )
    }

    override suspend fun getUserPrincipalByCredentials(organizationId: String, userName: String, password: String):
        UserPrincipal? {
        val user = usersService.authenticate(organizationId, userName, password)
        return UserPrincipal(
            TokenCredential(userName, TokenType.BASIC),
            user.hrn,
            principalPolicyService.fetchEntitlements(user.hrn)
        )
    }

    override suspend fun getUserPrincipalByCredentials(credentials: UsernamePasswordCredential): UserPrincipal {
        val validCredentials = credentials.validate()
        val user = usersService.authenticate(validCredentials.username, validCredentials.password)
        return UserPrincipal(
            TokenCredential(validCredentials.username, TokenType.BASIC),
            user.hrn,
            principalPolicyService.fetchEntitlements(user.hrn)
        )
    }
}

interface UserPrincipalService {
    suspend fun getUserPrincipalByRefreshToken(tokenCredential: TokenCredential): UserPrincipal?
    suspend fun getUserPrincipalByJwtToken(tokenCredential: TokenCredential): UserPrincipal?
    suspend fun getUserPrincipalByCredentials(organizationId: String, userName: String, password: String):
        UserPrincipal?

    suspend fun getUserPrincipalByCredentials(credentials: UsernamePasswordCredential): UserPrincipal
}

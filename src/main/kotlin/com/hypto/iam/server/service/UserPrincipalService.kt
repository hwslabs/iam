package com.hypto.iam.server.service

import com.hypto.iam.server.db.repositories.CredentialsRepo
import com.hypto.iam.server.security.TokenCredential
import com.hypto.iam.server.security.TokenType
import com.hypto.iam.server.security.UserPrincipal
import com.hypto.iam.server.security.UsernamePasswordCredential
import com.hypto.iam.server.utils.measureTimedValue
import com.hypto.iam.server.utils.policy.PolicyBuilder
import com.hypto.iam.server.validators.validate
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger("service.UserPrincipalServiceImpl")
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

    override suspend fun getUserPrincipalByJwtToken(
        tokenCredential: TokenCredential,
        deepCheck: Boolean
    ): UserPrincipal = measureTimedValue("TokenService.getUserPrincipalByJwtToken.$deepCheck", logger) {
        val token = tokenService.validateJwtToken(tokenCredential.value!!)
        val userHrnStr: String = token.body.get(TokenServiceImpl.USER_CLAIM, String::class.java)
        val entitlements: String = token.body.get(TokenServiceImpl.ENTITLEMENTS_CLAIM, String::class.java)
        return UserPrincipal(
            tokenCredential,
            userHrnStr,
            if (deepCheck) {
                principalPolicyService.fetchEntitlements(userHrnStr)
            } else {
                PolicyBuilder(entitlements)
            }
        )
    }

    override suspend fun getUserPrincipalByCredentials(organizationId: String, userName: String, password: String):
        UserPrincipal = measureTimedValue("TokenService.getUserPrincipalByCredentials", logger) {
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
    suspend fun getUserPrincipalByJwtToken(tokenCredential: TokenCredential, deepCheck: Boolean = false): UserPrincipal?
    suspend fun getUserPrincipalByCredentials(organizationId: String, userName: String, password: String):
        UserPrincipal?

    suspend fun getUserPrincipalByCredentials(credentials: UsernamePasswordCredential): UserPrincipal
}

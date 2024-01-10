package com.hypto.iam.server.authProviders

import com.hypto.iam.server.db.tables.records.UserAuthRecord
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential

interface BaseAuthProvider {
    val isVerifiedProvider: Boolean
    fun getProviderName(): String
    fun getProfileDetails(tokenCredential: TokenCredential): OAuthUserPrincipal
    suspend fun authenticate(principal: OAuthUserPrincipal, userAuthRecord: UserAuthRecord)
}

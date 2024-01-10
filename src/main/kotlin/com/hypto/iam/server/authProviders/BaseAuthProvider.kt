package com.hypto.iam.server.authProviders

import com.hypto.iam.server.db.tables.records.UserAuthRecord
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential

abstract class BaseAuthProvider {
    open val isVerifiedProvider: Boolean = true

    abstract fun getProviderName(): String

    abstract fun getProfileDetails(tokenCredential: TokenCredential): OAuthUserPrincipal

    open suspend fun authenticate(
        principal: OAuthUserPrincipal,
        userAuthRecord: UserAuthRecord,
    ) {
        return
    }
}

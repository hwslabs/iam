package com.hypto.iam.server.authProviders

import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential

abstract class BaseAuthProvider {
    constructor() {
        AuthProviderRegistry.registerProvider(provider = this)
    }
    abstract fun getProviderName(): String
    abstract fun getProfileDetails(tokenCredential: TokenCredential): OAuthUserPrincipal
}

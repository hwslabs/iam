package com.hypto.iam.server.authProviders

import com.hypto.iam.server.security.AuthMetadata
import com.hypto.iam.server.security.OAuthUserPrincipal
import com.hypto.iam.server.security.TokenCredential

interface BaseAuthProvider {
    fun getProviderName(): String
    fun getProfileDetails(tokenCredential: TokenCredential): OAuthUserPrincipal
    fun authenticate(principalMetadata: AuthMetadata? = null, authMetadata: AuthMetadata)
}

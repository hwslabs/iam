package com.hypto.iam.server.authProviders

import com.hypto.iam.server.security.AuthenticationException

object AuthProviderRegistry {
    private val providerRegistry: Map<String, BaseAuthProvider> = buildMap {
        put("Google", GoogleAuthProvider)
    }

    fun getProvider(providerName: String) = providerRegistry[providerName] ?: throw AuthenticationException(
        "Invalid issuer. No provider registered providerName=[$providerName]."
    )
}

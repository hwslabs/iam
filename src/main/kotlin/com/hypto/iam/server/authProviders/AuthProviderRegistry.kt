package com.hypto.iam.server.authProviders

object AuthProviderRegistry {
    private var providerRegistry: Map<String, BaseAuthProvider> = emptyMap()

    fun getProvider(providerName: String) = providerRegistry[providerName]

    fun registerProvider(provider: BaseAuthProvider) {
        providerRegistry = providerRegistry.plus(provider.getProviderName() to provider)
    }
}
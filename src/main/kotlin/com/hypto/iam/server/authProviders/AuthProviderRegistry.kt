package com.hypto.iam.server.authProviders

object AuthProviderRegistry {
    private var providerRegistry: Map<String, BaseAuthProvider> = emptyMap()

    init {
        registerProvider(GoogleAuthProvider)
        registerProvider(MicrosoftAuthProvider)
    }

    fun getProvider(providerName: String) = providerRegistry[providerName]

    fun registerProvider(provider: BaseAuthProvider) {
        providerRegistry = providerRegistry.plus(provider.providerName to provider)
    }
}

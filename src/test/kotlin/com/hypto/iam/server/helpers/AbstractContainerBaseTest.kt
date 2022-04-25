package com.hypto.iam.server.helpers

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.di.repositoryModule
import io.mockk.mockkClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient

abstract class AbstractContainerBaseTest : AutoCloseKoinTest() {
    protected var rootToken: String = ""
    protected lateinit var cognitoClient: CognitoIdentityProviderClient

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(repositoryModule, controllerModule, applicationModule)
    }

    @JvmField
    @RegisterExtension
    val koinMockProvider = MockProviderExtension.create { mockkClass(it) }

    @BeforeEach
    fun setup() {
        rootToken = getKoinInstance<AppConfig>().app.secretKey
        cognitoClient = mockCognitoClient()
    }

    init {
        PostgresInit
    }
}

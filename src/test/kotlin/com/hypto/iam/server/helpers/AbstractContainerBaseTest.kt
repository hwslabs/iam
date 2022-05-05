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
import software.amazon.awssdk.services.ses.SesClient

abstract class AbstractContainerBaseTest : AutoCloseKoinTest() {
    protected var rootToken: String = ""
    protected lateinit var cognitoClient: CognitoIdentityProviderClient
    protected lateinit var sesClient: SesClient

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
        mockPasscodeRepo()
        cognitoClient = mockCognitoClient()
        sesClient = mockSesClient()
    }

    init {
        PostgresInit
    }
}

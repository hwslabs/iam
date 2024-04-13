package com.hypto.iam.server.helpers

import com.google.gson.Gson
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.TestApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.test.inject

abstract class BaseSingleAppTest : AbstractContainerBaseTest() {
    protected val gson: Gson by inject()

    companion object {
        lateinit var testApp: TestApplication

        @JvmStatic
        @BeforeAll
        fun setupTest() {
            testApp =
                TestApplication {
                    environment {
                        config = ApplicationConfig("application-custom.conf")
                    }
                }
        }

        @JvmStatic
        @AfterAll
        fun teardownTest() {
            testApp.stop()
        }
    }
}

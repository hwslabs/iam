package com.hypto.iam.server.helpers

import org.koin.test.junit5.AutoCloseKoinTest

abstract class AbstractContainerBaseTest : AutoCloseKoinTest() {
    init {
        PostgresInit
    }
}

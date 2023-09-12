package com.hypto.iam.server.helpers

import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.mock.declareMock

fun KoinTest.mockOkHttpClient(): OkHttpClient = declareMock(named("AuthProvider")) {
    coEvery { newCall(any()) } returns mockk {
        coEvery { execute() } returns mockk {
            coEvery { isSuccessful } returns true
        }
    }
}

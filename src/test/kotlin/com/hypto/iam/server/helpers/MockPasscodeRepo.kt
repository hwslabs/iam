package com.hypto.iam.server.helpers

import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.models.VerifyEmailRequest
import io.mockk.coEvery
import org.koin.test.KoinTest
import org.koin.test.mock.declareMock

fun KoinTest.mockPasscodeRepo(): PasscodeRepo {
    return declareMock {
        coEvery {
            createPasscode(any())
        } coAnswers {
            firstArg()
        }
        coEvery {
            getValidPasscodeCount(any<String>(), any<VerifyEmailRequest.Purpose>())
        } returns 0
        coEvery {
            getValidPasscode(
                any<String>(),
                any<VerifyEmailRequest.Purpose>(),
                any<String>()
            )
        } coAnswers {
            PasscodesRecord().setId(firstArg()).setPurpose(secondArg<VerifyEmailRequest.Purpose>().toString())
                .setEmail(thirdArg())
        }
        coEvery { deleteByEmailAndPurpose(any<String>(), any<VerifyEmailRequest.Purpose>()) } returns true
    }
}

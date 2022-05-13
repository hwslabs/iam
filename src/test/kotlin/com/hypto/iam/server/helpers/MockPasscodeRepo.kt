package com.hypto.iam.server.helpers

import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.tables.pojos.Passcodes
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import com.hypto.iam.server.models.VerifyEmailRequest
import io.mockk.coEvery
import io.mockk.mockkObject
import java.time.LocalDateTime

fun mockPasscodeRepo() {
    mockkObject(PasscodeRepo)
    coEvery {
        PasscodeRepo.createPasscode(
            any<String>(),
            any<String>(),
            any<String>(),
            any<LocalDateTime>(),
            any<VerifyEmailRequest.Purpose>()
        )
    } coAnswers {
        PasscodesRecord().setId(firstArg()).setValidUntil(arg(3))
            .setPurpose(arg<VerifyEmailRequest.Purpose>(4).toString()).setEmail(secondArg())
            .setOrganizationId(thirdArg()).setCreatedAt(LocalDateTime.now())
    }
    coEvery {
        PasscodeRepo.findById(eq("testPasscode"))
    } coAnswers {
        Passcodes(
            firstArg(),
            LocalDateTime.now().plusDays(1),
            VerifyEmailRequest.Purpose.signup.toString(),
            "test@email.com",
            "testOrg",
            LocalDateTime.now()
        )
    }
    coEvery {
        PasscodeRepo.getValidPasscodeCount(any<String>(), any<VerifyEmailRequest.Purpose>())
    } returns 0
    coEvery {
        PasscodeRepo.getValidPasscode(
            any<String>(),
            any<VerifyEmailRequest.Purpose>(),
            any<String>()
        )
    } coAnswers {
        PasscodesRecord().setId(firstArg()).setPurpose(secondArg<VerifyEmailRequest.Purpose>().toString())
            .setEmail(thirdArg())
    }
    coEvery { PasscodeRepo.deleteById(any<String>()) } returns true
}

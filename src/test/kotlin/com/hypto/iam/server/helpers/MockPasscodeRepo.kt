package com.hypto.iam.server.helpers

import com.hypto.iam.server.db.repositories.PasscodeRepo
import com.hypto.iam.server.db.tables.records.PasscodesRecord
import io.mockk.coEvery
import io.mockk.mockkObject
import java.time.LocalDateTime
import java.util.UUID

fun mockPasscodeRepo() {
    mockkObject(PasscodeRepo)
    coEvery {
        PasscodeRepo.createPasscode(
            any<String>(),
            any<String>(),
            any<LocalDateTime>(),
            any<PasscodeRepo.Type>()
        )
    } coAnswers {
        PasscodesRecord().setId(UUID.randomUUID()).setValidUntil(thirdArg())
            .setType(arg<PasscodeRepo.Type>(3).toString()).setEmail(firstArg())
            .setOrganizationId(secondArg()).setCreatedAt(LocalDateTime.now())
    }
    coEvery { PasscodeRepo.getValidPasscode(any<UUID>(), any<PasscodeRepo.Type>(), any<String>()) } coAnswers {
        PasscodesRecord().setId(firstArg()).setType(secondArg<PasscodeRepo.Type>().toString()).setEmail(thirdArg())
    }
    coEvery { PasscodeRepo.deleteById(any<UUID>()) } returns true
}

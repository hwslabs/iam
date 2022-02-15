package com.hypto.iam.server

import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.service.TokenService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ConfigurationTest {
    @Test
    fun `Sample Test`() {
        Assertions.assertEquals(1, 1)

        MasterKeysRepo.rotateKey()
        println(TokenService.generateJwtToken("u", "o"))
    }
}

package com.hypto.iam.server

import com.hypto.iam.server.db.repositories.MasterKeysRepo
import com.hypto.iam.server.db.tables.records.OrganizationsRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.KoinTest
import java.time.LocalDateTime

internal class ConfigurationTest : KoinTest {

    @Test
    fun `Sample Test`() {
        Assertions.assertEquals(1, 1)

//        MasterKeysRepo.rotateKey()

//        val org = OrganizationsRecord("id1", "name", "description", null, LocalDateTime.now(), LocalDateTime.now())
//        org.insert()
//        println(org.toString())
//        org.name = "name2"
//        org.update()
//        println(org.toString())

//        startKoin {
//            modules(repositoryModule, controllerModule, applicationModule)
//        }
//
//        runBlocking {
//    // Error-ing out because of 2 different koins in class path: io.insert-koin:koin-core-jvm and org.koin.koin-core
//            val tokenService: TokenService by inject()
//            println(tokenService.generateJwtToken(Hrn.of("o", "u", "i")))
//        }
//
//        stopKoin()
    }
}

package com.hypto.iam.server

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.KoinTest

// TODO: [IMPORTANT] Cleanup / delete this file
internal class ConfigurationTest : KoinTest {

    @Test
    fun `Sample Test`() {
//        startKoin {
//            modules(repositoryModule, controllerModule, applicationModule)
//        }
        Assertions.assertEquals(1, 1)

//        val hrnFactor = HrnFactory()

//        fun genAuditEntry(): AuditEntries {
//            return AuditEntries(
//                UUID.randomUUID(),
//                UUID.randomUUID().toString(),
//                LocalDateTime.now(), "principal", "organization", "resource", "operation", null)
//        }
// //        val entries = mutableListOf<AuditEntries>()
//
//        for (i in 1..5) {
//            val entry = genAuditEntry()
//            KotlinLogging.auditLog(entry)
// //            entries.add(entry)
//        }

//        AuditEntriesRepo.batchInsert(entries)

//        fun genBody(): String {
//            return gson.toJson(genAuditEntry())
//        }

//        fun getSQSMessage(): SQSEvent.SQSMessage {
//            val msg = SQSEvent.SQSMessage()
//            msg.body = genBody()
//            return msg
//        }

//        val event = SQSEvent()
//        event.records = listOf<SQSEvent.SQSMessage>()

//        for (i in 1..5) {
//            val a = getSQSMessage()
//            event.records.add(a)
//        }
//
//        AuditEventHandler().handleRequest(event, null)

//        ========

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

package com.hypto.iam.server.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.AuditEntriesRepo
import com.hypto.iam.server.db.tables.pojos.AuditEntries
import com.hypto.iam.server.di.getKoinInstance
import kotlinx.coroutines.runBlocking

class AuditEventHandler : RequestHandler<SQSEvent, Unit> {
    private val auditEntryRepo: AuditEntriesRepo = getKoinInstance()

    override fun handleRequest(
        input: SQSEvent,
        context: Context?,
    ) {
        val entries = input.records.map { auditEntriesFrom(it) }

        val state =
            runBlocking {
                auditEntryRepo.batchInsert(entries)
            }

        // TODO: Verify if the entry already exists in case of failed messages and ignore them.
        if (!state) {
            throw Exception("few Batch inserts failed")
        }
    }

    companion object {
        val gson: Gson = getKoinInstance()

        fun auditEntriesFrom(message: SQSEvent.SQSMessage): AuditEntries {
            return gson.fromJson(message.body, AuditEntries::class.java)
        }
    }
}

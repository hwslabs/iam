package com.hypto.iam.server.extensions

import com.google.gson.Gson
import com.hypto.iam.server.db.tables.pojos.AuditEntries
import com.hypto.iam.server.di.getKoinInstance
import mu.KLogger
import mu.KotlinLogging
import mu.toKLogger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

private const val AUDIT_LOGGER_NAME = "audit-logger"
private val auditLogger: KLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME).toKLogger()
private val auditMarker = MarkerFactory.getMarker("AUDIT")
private val gson: Gson = getKoinInstance()

fun KotlinLogging.auditLog(auditEntry: AuditEntries) {
    return auditLogger.info(auditMarker, gson.toJson(auditEntry))
}

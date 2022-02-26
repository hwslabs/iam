package com.hypto.iam.server.extensions

import mu.KLogger
import mu.KotlinLogging
import mu.toKLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val AUDIT_LOGGER_NAME = "audit-logger"
private val auditLogger: Logger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)

fun KotlinLogging.auditLogger(): KLogger {
    return auditLogger.toKLogger()
}

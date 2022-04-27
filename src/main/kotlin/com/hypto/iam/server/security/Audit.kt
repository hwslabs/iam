@file:Suppress("TooGenericExceptionCaught")

package com.hypto.iam.server.security

import com.google.gson.Gson
import com.hypto.iam.server.db.tables.pojos.AuditEntries
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.features.globalcalldata.GlobalCallData.Feature.globalCallDataCleanupPhase
import com.hypto.iam.server.features.globalcalldata.GlobalCallData.Feature.globalCallDataPhase
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.userAgent
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import java.time.LocalDateTime
import mu.KLogger
import mu.KotlinLogging
import mu.toKLogger
import org.jooq.JSON
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

private val logger = KotlinLogging.logger { }

class AuditException(override val message: String) : Exception(message)

/**
 * This class is used in api request flow. These records audit info for the performed action
 * TODO: Support auditing custom resource-actions as well.
 *       Currently, Audit module records only requests pertaining to IAM module.
 */
class Audit(config: Configuration) : KoinComponent {
    private val enabled: Boolean = config.enabled
    private val logger = KotlinLogging.logger { }

    class Configuration {
        internal var enabled: Boolean = true
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, Audit> {
        override val key = AttributeKey<Audit>("AuditFeature")
        val AuditContextKey = AttributeKey<AuditContext>("AuditContext")

        private val auditPhase = PipelinePhase("Audit")
        private val auditCommitPhase = PipelinePhase("AuditCommit")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): Audit {
            val configuration = Configuration().apply(configure)

            return Audit(configuration).also { auditFeature ->
                pipeline.insertPhaseAfter(globalCallDataPhase, auditPhase)
                pipeline.intercept(auditPhase) {
                    auditFeature.interceptBeforeReceive(this)
                }

                pipeline.sendPipeline.insertPhaseBefore(globalCallDataCleanupPhase, auditCommitPhase)
                pipeline.sendPipeline.intercept(auditCommitPhase) {
                    auditFeature.interceptAfterSend(this, it)
                }
            }
        }
    }

    private fun interceptBeforeReceive(context: PipelineContext<Unit, ApplicationCall>) {
        if (!enabled) {
            return
        }
        context.call.attributes.put(AuditContextKey, AuditContext(context))
    }

    @Suppress("TooGenericExceptionCaught")
    private fun interceptAfterSend(pipelineContext: PipelineContext<Any, ApplicationCall>, message: Any) {
        if (!enabled) { return }
        try {
            val auditContext = pipelineContext.call.attributes[AuditContextKey]
            auditContext.persist(message)
        } catch (e: Exception) {
            logger.warn(e) { "Exception occurred in audit module" }
        }
    }
}

@Suppress("FunctionOnlyReturningConstant")
suspend fun auditLog(): AuditContext? {
    // TODO: Fix this when getting audit logging to work.
    return null
//    return callData().call.attributes.getOrNull(Audit.AuditContextKey)
}

class AuditContext(val context: PipelineContext<Unit, ApplicationCall>) {
    companion object {
        private const val AUDIT_LOGGER_NAME = "audit-logger"
        private val auditLogger: KLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME).toKLogger()
        private val auditMarker = MarkerFactory.getMarker("AUDIT")
        private val gson: Gson = getKoinInstance()
    }
    val entries = mutableListOf<ResourceHrn>()
    private val logger = KotlinLogging.logger { }

    fun append(resourceHrn: ResourceHrn): Boolean {
        return try {
            entries.add(resourceHrn)
        } catch (e: Exception) {
            logger.error(e) { "Failed to append resource to audit log" }
            false
        }
    }

    fun persist(message: Any) {
        try {
            entries.forEach {
                persistEntry(context.call, message, it.toString())
            }
        } catch (e: Exception) {
            logger.error("Audit error", e)
        }
    }

    private fun fetchStatusCode(message: Any): HttpStatusCode? {
        return when (message) {
            is TextContent -> message.status
            else -> null
        }
    }

    private fun persistEntry(applicationCall: ApplicationCall, message: Any, resourceHrnStr: String) {
        val principalHrn = applicationCall.principal<UserPrincipal>()?.hrn as ResourceHrn?
            ?: throw IllegalStateException("User principal missing in application context")
        applicationCall.callId ?: throw IllegalStateException("Request id missing in application context")

        val meta = hashMapOf(
            Pair("HttpMethod", applicationCall.request.httpMethod.value),
            Pair("Referer", applicationCall.request.userAgent()), // TODO: [IMPORTANT] Verify this is not ALB address
            Pair("StatusCode", fetchStatusCode(message)?.value.toString())
        )

        val auditEntry = AuditEntries(
            null,
            applicationCall.callId!!,
            LocalDateTime.now(),
            principalHrn.toString(),
            principalHrn.organization,
            resourceHrnStr,
            applicationCall.request.path(),
            JSON.valueOf(gson.toJson(meta))
        )

        return auditLogger.info(auditMarker, gson.toJson(auditEntry))
    }
}

@file:Suppress("TooGenericExceptionCaught")

package com.hypto.iam.server.security

import com.google.gson.Gson
import com.hypto.iam.server.db.tables.pojos.AuditEntries
import com.hypto.iam.server.di.getKoinInstance
import com.hypto.iam.server.features.globalcalldata.GlobalCallData.Feature.globalCallDataCleanupPhase
import com.hypto.iam.server.features.globalcalldata.GlobalCallData.Feature.globalCallDataPhase
import com.hypto.iam.server.features.globalcalldata.callData
import com.hypto.iam.server.utils.ResourceHrn
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.content.TextContent
import io.ktor.features.callId
import io.ktor.http.HttpStatusCode
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.userAgent
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
 * This class is used in api request flow. This records audit info for the performed action
 */
class Audit(config: Configuration) : KoinComponent {
    private val enabled: Boolean = config.enabled

    class Configuration {
        internal var enabled: Boolean = true
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Audit> {
        override val key = AttributeKey<Audit>("AuditFeature")
        val AuditContextKey = AttributeKey<AuditContext>("AuditContext")

        val auditPhase = PipelinePhase("Audit")
        val auditCommitPhase = PipelinePhase("AuditCommit")

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
        if (!enabled) { return }
        context.call.attributes.put(AuditContextKey, AuditContext(context))
    }

    private fun interceptAfterSend(pipelineContext: PipelineContext<Any, ApplicationCall>, message: Any) {
        if (!enabled) { return }
        val auditContext = pipelineContext.call.attributes[AuditContextKey]
        auditContext.persist(message)
    }
}

suspend fun auditLog(): AuditContext {
    return callData().call.attributes[Audit.AuditContextKey]
}

private const val AUDIT_LOGGER_NAME = "audit-logger"
private val auditLogger: KLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME).toKLogger()
private val auditMarker = MarkerFactory.getMarker("AUDIT")
private val gson: Gson = getKoinInstance()

class AuditContext(val context: PipelineContext<Unit, ApplicationCall>) {
    val entries = mutableListOf<ResourceHrn>()

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

    private fun persistEntry(applicationCall: ApplicationCall, message: Any, resource: String) {
        val principalHrn = applicationCall.principal<UserPrincipal>()?.hrn as ResourceHrn?
            ?: throw IllegalStateException("User principal missing in application context")
        applicationCall.callId ?: throw IllegalStateException("Request id missing in application context")

        val meta = hashMapOf(
            Pair("HttpMethod", applicationCall.request.httpMethod.value),
            Pair("Referer", applicationCall.request.userAgent()), // TODO: Verify this is not ALB address
            Pair("StatusCode", fetchStatusCode(message)?.value.toString())
        )

        val auditEntry = AuditEntries(
            null,
            applicationCall.callId!!,
            LocalDateTime.now(),
            principalHrn.toString(),
            principalHrn.organization,
            resource,
            applicationCall.request.path(),
            JSON.valueOf(gson.toJson(meta))
        )

        return auditLogger.info(auditMarker, gson.toJson(auditEntry))
    }
}

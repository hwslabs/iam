package com.hypto.iam.server.features.globalcalldata

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase

class GlobalCallData(configuration: Configuration) {

    class Configuration

    private fun interceptBeforeReceive(context: PipelineContext<Unit, ApplicationCall>) {
        callCache.create(context.coroutineContext)
        callCache.set(context.coroutineContext, Call, context.call)
    }

    private fun interceptAfterSend(context: PipelineContext<Any, ApplicationCall>) {
        callCache.remove(context.coroutineContext)
    }

    /**
     * Installable feature for [GlobalCallData].
     */
    companion object Feature : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, GlobalCallData> {
        override val key = AttributeKey<GlobalCallData>("GlobalCallData")
        val callCache = CallCache()
        var enabled = false

        val globalCallDataPhase = PipelinePhase("GlobalCallData")
        val globalCallDataCleanupPhase = PipelinePhase("GlobalCallDataCleanup")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): GlobalCallData {
            val configuration = Configuration().apply(configure)

            return GlobalCallData(configuration).also { callDataFeature ->
                pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, globalCallDataPhase)
                pipeline.intercept(globalCallDataPhase) {
                    callDataFeature.interceptBeforeReceive(this)
                }

                pipeline.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.After, globalCallDataCleanupPhase)
                pipeline.sendPipeline.intercept(globalCallDataCleanupPhase) {
                    callDataFeature.interceptAfterSend(this)
                }

                enabled = true
            }
        }
    }
}

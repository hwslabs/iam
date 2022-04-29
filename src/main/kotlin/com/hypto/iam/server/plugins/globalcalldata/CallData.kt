package com.hypto.iam.server.plugins.globalcalldata

import io.ktor.server.application.ApplicationCall
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface Key<E : Any>
object Call : Key<ApplicationCall>

open class CallData(context: CoroutineContext) {
    private val delegate = CallDataDelegate(context)
    val call: ApplicationCall by delegate.propNotNull(Call)
}

suspend fun callData(): CallData {
    if (!GlobalCallData.enabled) {
        throw IllegalAccessException("GlobalCallData Feature is not enabled!")
    }

    return CallData(coroutineContext)
}

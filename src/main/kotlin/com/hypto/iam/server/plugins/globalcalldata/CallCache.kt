package com.hypto.iam.server.plugins.globalcalldata

import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class CallCache {
    private val data = ConcurrentHashMap<CoroutineContext, MutableMap<Any, Any>>()

    fun create(context: CoroutineContext) {
        data[context] = hashMapOf()
    }

    fun <V : Any> set(
        context: CoroutineContext,
        key: Key<V>,
        value: V,
    ) {
        data[context]?.put(key, value)
    }

    fun <V : Any> get(
        context: CoroutineContext,
        key: Key<V>,
    ): V? {
        return data[context]?.get(key) as V?
    }

    fun remove(context: CoroutineContext) {
        data.remove(context)
    }
}

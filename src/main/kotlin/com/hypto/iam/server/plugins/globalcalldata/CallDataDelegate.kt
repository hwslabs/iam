package com.hypto.iam.server.plugins.globalcalldata

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty

class CallDataDelegate(private val context: CoroutineContext) {
    fun <V : Any> prop(key: Key<V>): DelegateProperty<V> {
        return DelegateProperty(key)
    }

    fun <V : Any> propNotNull(key: Key<V>): DelegatePropertyNotNull<V> {
        return DelegatePropertyNotNull(key)
    }

    inner class DelegateProperty<V : Any>(private val key: Key<V>) {
        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): V? {
            return GlobalCallData.callCache.get(context, key)
        }

        operator fun setValue(
            thisRef: Any?,
            property: KProperty<*>,
            value: V,
        ) {
            GlobalCallData.callCache.set(context, key, value)
        }
    }

    inner class DelegatePropertyNotNull<V : Any>(private val key: Key<V>) {
        private val delegate = DelegateProperty(key)

        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): V {
            return delegate.getValue(thisRef, property)!!
        }

        operator fun setValue(
            thisRef: Any?,
            property: KProperty<*>,
            value: V,
        ) {
            delegate.setValue(thisRef, property, value)
        }
    }
}

package com.hypto.iam.server.utils

import com.hypto.iam.server.di.getKoinInstance
import io.micrometer.core.instrument.MeterRegistry
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration
import mu.KLogger

val microMeterRegistry: MeterRegistry = getKoinInstance()
inline fun measureTime(name: String, logger: KLogger, block: () -> Unit) = measureTimedValue(name, logger, block)

@OptIn(ExperimentalTime::class)
inline fun <T> measureTimedValue(name: String, logger: KLogger, block: () -> T): T {
    val timedValue = measureTimedValue(block)
    microMeterRegistry.timer(name).record(timedValue.duration.toJavaDuration())
    logger.info { "[Timing] Operation=[$name] | Time=[${timedValue.duration.inWholeNanoseconds} ns]" }
    return timedValue.value
}

package com.hypto.iam.server.extensions

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun LocalDateTime.toUTCOffset(): OffsetDateTime {
    return this.atOffset(ZoneOffset.UTC)
}

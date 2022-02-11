package com.hypto.iam.server.utils

import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

object IdUtil {
    val ulid = ULID()

    enum class IdCharset(val seed: String) {
        UPPERCASE_ALPHABETS("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        LOWERCASE_ALPHABETS(UPPERCASE_ALPHABETS.seed.lowercase()),
        NUMERIC("0123456789"),
        LOWER_ALPHANUMERIC(LOWERCASE_ALPHABETS.seed + NUMERIC.seed),
        UPPER_ALPHANUMERIC(UPPERCASE_ALPHABETS.seed + NUMERIC.seed),
        ALPHABETS(UPPERCASE_ALPHABETS.seed + LOWERCASE_ALPHABETS.seed),
        ALPHANUMERIC(ALPHABETS.seed + NUMERIC.seed),
    }

    fun randomId(length: Long = 10, charset: IdCharset = IdCharset.UPPERCASE_ALPHABETS): String {
        return ThreadLocalRandom.current().ints(length, 0, charset.seed.length)
            .asSequence()
            .map(charset.seed::get)
            .joinToString("")
    }

    object Ulid {
        fun getId(timestamp: Long = System.currentTimeMillis()): String {
            return ulid.nextULID(timestamp)
        }

        fun timestamp(ulid: String): Long {
            return ULID.parseULID(ulid).timestamp()
        }
    }
}

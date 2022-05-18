package com.hypto.iam.server.utils

import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

object IdGenerator {

    enum class Charset(val seed: String) {
        UPPERCASE_ALPHABETS("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        LOWERCASE_ALPHABETS(UPPERCASE_ALPHABETS.seed.lowercase()),
        NUMERIC("0123456789"),
        LOWER_ALPHANUMERIC(LOWERCASE_ALPHABETS.seed + NUMERIC.seed),
        UPPER_ALPHANUMERIC(UPPERCASE_ALPHABETS.seed + NUMERIC.seed),
        ALPHABETS(UPPERCASE_ALPHABETS.seed + LOWERCASE_ALPHABETS.seed),
        ALPHANUMERIC(ALPHABETS.seed + NUMERIC.seed),
    }

    fun randomId(length: Long = 10, charset: Charset = Charset.UPPERCASE_ALPHABETS): String {
        return ThreadLocalRandom.current().ints(length, 0, charset.seed.length)
            .asSequence()
            .map(charset.seed::get)
            .joinToString("")
    }

    /**
     * Convert numbers to required charset.
     *
     * E.g: For number = 1645204287
     * - numberToId(number, IdGenerator.Charset.NUMERIC) == "1645204287"
     * - numberToId(number, IdGenerator.Charset.ALPHABETS) == "ERAhbz"
     * - numberToId(number, IdGenerator.Charset.ALPHANUMERIC) == "BxVGxB"
     * - numberToId(number, IdGenerator.Charset.UPPERCASE_ALPHABETS) == "FIMFEDZ"
     * - numberToId(number, IdGenerator.Charset.LOWERCASE_ALPHABETS) == "fimfedz"
     * - numberToId(number, IdGenerator.Charset.UPPER_ALPHANUMERIC) == "1HSP1D"
     * - numberToId(number, IdGenerator.Charset.LOWER_ALPHANUMERIC) == "1hsp1d"
     */
    fun numberToId(number: Long, charset: Charset = Charset.UPPERCASE_ALPHABETS): String {
        return if (number < 0L) {
            "-" + numberToId(-number - 1)
        } else if (number == 0L) {
            charset.seed[number.toInt()].toString()
        } else {
            val seed = charset.seed
            var quot = number
            val builder = StringBuilder()

            while (quot != 0L) {
                builder.append(seed[(quot % seed.length).toInt()])
                quot /= seed.length
            }
            builder.reverse().toString()
        }
    }

    private const val MIN_TIME_BASED_RANDOM_ID_SIZE = 10

    fun timeBasedRandomId(length: Long = 10, charset: Charset = Charset.UPPERCASE_ALPHABETS): String {
        if (length < MIN_TIME_BASED_RANDOM_ID_SIZE) {
            throw IllegalArgumentException(
                "Cannot generate a timestamp based random id with less than $MIN_TIME_BASED_RANDOM_ID_SIZE characters"
            )
        }
        val timeId = numberToId(Instant.now().toEpochMilli(), charset)

        return timeId + randomId(length - timeId.length, charset)
    }
}

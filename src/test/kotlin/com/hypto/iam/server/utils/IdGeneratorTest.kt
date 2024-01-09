package com.hypto.iam.server.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

class IdGeneratorTest {
    private val subject = IdGenerator

    @Test
    fun `Test length based ID generation`() {
        Assertions.assertEquals(10, subject.randomId().length) // Default length
        Assertions.assertEquals(3, subject.randomId(length = 3).length)
        Assertions.assertEquals(12, subject.randomId(length = 12).length)
        Assertions.assertEquals(55, subject.randomId(length = 55).length)
        Assertions.assertEquals(100, subject.randomId(length = 100).length)
    }

    @Test
    fun `Test Charset based ID generation`() {
        val rand1 = subject.randomId(charset = IdGenerator.Charset.UPPERCASE_ALPHABETS)
        Assertions.assertEquals(rand1.uppercase(), rand1)
        Assertions.assertTrue(rand1.all { it.isLetter() })

        val rand2 = subject.randomId(charset = IdGenerator.Charset.LOWERCASE_ALPHABETS)
        Assertions.assertEquals(rand2.lowercase(), rand2)
        Assertions.assertTrue(rand2.all { it.isLetter() })

        val rand3 = subject.randomId(charset = IdGenerator.Charset.NUMERIC)
        Assertions.assertTrue(rand3.all { it.isDigit() })

        val rand4 = subject.randomId(charset = IdGenerator.Charset.LOWER_ALPHANUMERIC)
        Assertions.assertTrue(rand4.all { it.isDigit() || it.isLowerCase() })

        val rand5 = subject.randomId(charset = IdGenerator.Charset.UPPER_ALPHANUMERIC)
        Assertions.assertTrue(rand5.all { it.isDigit() || it.isUpperCase() })

        val rand6 = subject.randomId(charset = IdGenerator.Charset.ALPHABETS)
        Assertions.assertTrue(rand6.all { it.isLetter() })

        val rand7 = subject.randomId(charset = IdGenerator.Charset.ALPHANUMERIC)
        Assertions.assertTrue(rand7.all { it.isDigit() || it.isLetter() })
    }

    @Test
    fun `Test numberToId`() {
        for (charSet in IdGenerator.Charset.values()) {
            for (i in 0 until charSet.seed.length) {
                Assertions.assertEquals(
                    subject.numberToId(i.toLong(), charSet),
                    charSet.seed[i].toString(),
                )
            }
        }

        val number = 1645204287L

        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.NUMERIC), "1645204287")
        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.ALPHABETS), "ERAhbz")
        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.ALPHANUMERIC), "BxVGxB")
        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.UPPERCASE_ALPHABETS), "FIMFEDZ")
        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.LOWERCASE_ALPHABETS), "fimfedz")
        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.UPPER_ALPHANUMERIC), "1HSP1D")
        Assertions.assertEquals(subject.numberToId(number, IdGenerator.Charset.LOWER_ALPHANUMERIC), "1hsp1d")
    }

    @Test
    fun `Test timeBasedRandomId`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) { subject.timeBasedRandomId(length = 5) }

        Assertions.assertEquals(subject.timeBasedRandomId(10).length, 10)

        val id1 = subject.timeBasedRandomId()
        sleep(1)
        val id2 = subject.timeBasedRandomId()
        Assertions.assertTrue(id2 > id1)
    }
}

package com.hypto.iam.server.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class IdUtilTest {
    private val subject = IdUtil

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
        val rand1 = subject.randomId(charset = IdUtil.IdCharset.UPPERCASE_ALPHABETS)
        Assertions.assertEquals(rand1.uppercase(), rand1)
        Assertions.assertTrue(rand1.all { it.isLetter() })

        val rand2 = subject.randomId(charset = IdUtil.IdCharset.LOWERCASE_ALPHABETS)
        Assertions.assertEquals(rand2.lowercase(), rand2)
        Assertions.assertTrue(rand2.all { it.isLetter() })

        val rand3 = subject.randomId(charset = IdUtil.IdCharset.NUMERIC)
        Assertions.assertTrue(rand3.all { it.isDigit() })

        val rand4 = subject.randomId(charset = IdUtil.IdCharset.LOWER_ALPHANUMERIC)
        Assertions.assertTrue(rand4.all { it.isDigit() || it.isLowerCase() })

        val rand5 = subject.randomId(charset = IdUtil.IdCharset.UPPER_ALPHANUMERIC)
        Assertions.assertTrue(rand5.all { it.isDigit() || it.isUpperCase() })

        val rand6 = subject.randomId(charset = IdUtil.IdCharset.ALPHABETS)
        Assertions.assertTrue(rand6.all { it.isLetter() })

        val rand7 = subject.randomId(charset = IdUtil.IdCharset.ALPHANUMERIC)
        Assertions.assertTrue(rand7.all { it.isDigit() || it.isLetter() })
    }
}

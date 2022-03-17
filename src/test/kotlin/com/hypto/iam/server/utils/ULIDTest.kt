package com.hypto.iam.server.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ULIDTest {
    @Test
    fun `Test ULID generation - Format`() {
        val subject = ULID()
        val ulidString = subject.nextULID()

        Assertions.assertNotNull(ulidString)
        Assertions.assertNotEquals(ulidString, subject.nextULID())
        Assertions.assertEquals(ulidString.uppercase(), ulidString)
        Assertions.assertEquals(ulidString.length, 26)
    }

    @Test
    fun `Test ULID generation - Randomness`() {
        val subject = ULID()
        val ulidString1 = subject.nextULID()
        val ulidString2 = subject.nextULID()

        Assertions.assertNotEquals(ulidString1, ulidString2)
    }

    @Test
    fun `Test ULID - Monotonicity`() {
        val subject = ULID()
        val timestamp = System.currentTimeMillis()

        val ulidString1 = subject.nextULID(timestamp)
        val ulidValue1 = ULID.parseULID(ulidString1)
        val ulidValue2 = subject.nextMonotonicValue(ulidValue1, timestamp)
        val ulidString2 = ulidValue2.toString()

        Assertions.assertNotEquals(ulidString1, ulidString2)

        Assertions.assertEquals(ulidString1, ulidValue1.toString())
        Assertions.assertEquals(ulidString2, ulidValue2.toString())

        Assertions.assertFalse(ulidValue1.equals(ulidValue2))
        Assertions.assertTrue(ulidValue1 < ulidValue2)

        val ulidValue3 = ulidValue2.increment()
        Assertions.assertTrue(ulidValue2 < ulidValue3)

        val ulidValue4 = subject.nextValue(timestamp)
        Assertions.assertNotEquals(ulidValue1, ulidValue4)
    }

    @Test
    fun `Test ULID parsing`() {
        val subject = ULID()
        val timestamp = System.currentTimeMillis()
        val ulidString = subject.nextULID(timestamp)

        val parsedUlid = ULID.parseULID(ulidString)

        Assertions.assertEquals(timestamp, parsedUlid.timestamp())
    }
}

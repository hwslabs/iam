package com.hypto.iam.server.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ULIDTest {
    @Test
    fun `Test ULID generation`() {
        val subject = ULID()
        val ulidString = subject.nextULID()

        Assertions.assertNotNull(ulidString)
        Assertions.assertNotEquals(ulidString, subject.nextULID())
        Assertions.assertEquals(ulidString.uppercase(), ulidString)
        Assertions.assertEquals(ulidString.length, 26)
    }
}

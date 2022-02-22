package com.hypto.iam.server.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class HrnTest {
    @Test
    fun `Test hrn initialization from individual components`() {
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of("") }
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of("hws:iam::user") }
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of("hws:iam:user") }
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of("hws::id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of("hws:id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of(":iam:id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { Hrn.of("iam:id1:user") }

        Assertions.assertDoesNotThrow { Hrn.of("hws:iam:id1:user/user1") }
        Assertions.assertDoesNotThrow { Hrn.of("hws:iam:id1:user") }
    }
}

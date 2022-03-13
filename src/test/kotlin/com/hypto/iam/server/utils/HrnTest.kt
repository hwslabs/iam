package com.hypto.iam.server.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HrnTest {
    @Test
    fun `Test hrn factory`() {
        // Invalid Hrn formats
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("hrn:iam:::user1") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("hrn:iam:user") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("hrn::id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("hrn:id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn(":iam:id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("iam:id1:user") }
        Assertions.assertThrows(HrnParseException::class.java) { HrnFactory.getHrn("hrn:id1:\$user") }

        val resourceInstanceHrn = HrnFactory.getHrn("hrn:hypto::iam-resource/12345")
        assert(resourceInstanceHrn is ResourceHrn) { "Resource instance hrn must be of type ResourceHrn" }

        val userInstanceHrn = HrnFactory.getHrn("hrn:hypto::iam-user/12345")
        assert(userInstanceHrn is ResourceHrn) { "User instance hrn must be of type ResourceHrn" }

        val resourceHrn = HrnFactory.getHrn("hrn:hypto::ledger")
        assert(resourceHrn is ResourceHrn) { "Global resources are still Resources, so type should be ResourceHrn" }
        assertEquals("", (resourceHrn as ResourceHrn).resourceInstance)

        val actionHrn = HrnFactory.getHrn("hrn:hypto::ledger\$addTransaction")
        assert(actionHrn is ActionHrn) { "Operations are global, so type should be of GlobalHrn" }
    }
}

package org.carlospinan.bloqueador.app.blocklist

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuplicateWarningTest {
    @Test
    fun `should warn when number is in allowlist`() {
        val allowlist = setOf("+34600123456", "+34900999999")
        assertTrue(shouldWarnOnAdd("+34600123456", allowlist))
    }

    @Test
    fun `should not warn when number is not in allowlist`() {
        val allowlist = setOf("+34600123456")
        assertFalse(shouldWarnOnAdd("+34600987654", allowlist))
    }

    @Test
    fun `should not warn when allowlist is empty`() {
        assertFalse(shouldWarnOnAdd("+34600123456", emptySet()))
    }

    @Test
    fun `should not warn when number is empty`() {
        assertFalse(shouldWarnOnAdd("", setOf("+34600123456")))
    }

    companion object {
        private fun shouldWarnOnAdd(
            number: String,
            allowlistedNumbers: Set<String>,
        ): Boolean = number.isNotBlank() && number in allowlistedNumbers
    }
}

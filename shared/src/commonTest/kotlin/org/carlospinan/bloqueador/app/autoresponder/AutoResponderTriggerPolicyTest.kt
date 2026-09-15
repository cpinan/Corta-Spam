package org.carlospinan.bloqueador.app.autoresponder

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoResponderTriggerPolicyTest {
    @Test
    fun disabled_neverGreets() {
        val config = AutoResponderConfig(enabled = false, script = "Hello")
        assertFalse(AutoResponderTriggerPolicy.shouldGreet(config, hasOtherLiveCall = false))
    }

    @Test
    fun enabledButInvalid_neverGreets() {
        val config = AutoResponderConfig(enabled = true, script = "", audioUri = "")
        assertFalse(AutoResponderTriggerPolicy.shouldGreet(config, hasOtherLiveCall = false))
    }

    @Test
    fun enabledAndValid_withAnotherLiveCall_doesNotGreet() {
        val config = AutoResponderConfig(enabled = true, script = "Hello")
        assertFalse(AutoResponderTriggerPolicy.shouldGreet(config, hasOtherLiveCall = true))
    }

    @Test
    fun enabledAndValid_withNoOtherCall_greets() {
        val config = AutoResponderConfig(enabled = true, script = "Hello")
        assertTrue(AutoResponderTriggerPolicy.shouldGreet(config, hasOtherLiveCall = false))
    }
}

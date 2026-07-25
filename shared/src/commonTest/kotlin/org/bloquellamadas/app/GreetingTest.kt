package org.bloquellamadas.app

import kotlin.test.Test
import kotlin.test.assertTrue

class GreetingTest {
    @Test
    fun greetingMentionsTheApp() {
        assertTrue(greeting().contains("BloqueaLlamadas"))
    }
}

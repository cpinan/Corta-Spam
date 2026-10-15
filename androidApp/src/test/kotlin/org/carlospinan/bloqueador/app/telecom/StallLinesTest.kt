package org.carlospinan.bloqueador.app.telecom

import kotlin.test.Test
import kotlin.test.assertEquals

class StallLinesTest {
    @Test
    fun `there is one string resource for every line the policy can pick`() {
        assertEquals(StallPolicy.LINE_COUNT, StallLines.IDS.size)
    }
}

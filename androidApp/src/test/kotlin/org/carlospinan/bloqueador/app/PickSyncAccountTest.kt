package org.carlospinan.bloqueador.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PickSyncAccountTest {
    private val me = "me@gmail.com" to "com.google"

    @Test
    fun `no contacts means no preference`() {
        assertNull(pickSyncAccount(emptyList()))
    }

    @Test
    fun `device-only and sim contacts are never chosen`() {
        assertNull(pickSyncAccount(listOf("Phone" to "com.android.local", "SIM" to "com.android.sim")))
    }

    @Test
    fun `the google account holding most contacts wins`() {
        val other = "work@gmail.com" to "com.google"
        val picked = pickSyncAccount(listOf(other, me, me, me, "Phone" to "com.android.local", "x" to "com.android.local"))
        assertEquals(me, picked)
    }
}

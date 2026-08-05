package org.carlospinan.bloqueador.app.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteSectionTest {
    @Test
    fun `home route maps to index 0`() {
        assertEquals(0, routeSection(AdaptiveRoutes.HOME))
        assertEquals(0, routeSection(AdaptiveRoutes.STATS))
    }

    @Test
    fun `call log route maps to index 1`() {
        assertEquals(1, routeSection(AdaptiveRoutes.CALL_LOG))
        assertEquals(1, routeSection("call_log/all"))
        assertEquals(1, routeSection("call_log/today"))
        assertEquals(1, routeSection("call_log/week"))
    }

    @Test
    fun `block list hub and detail routes map to index 2`() {
        assertEquals(2, routeSection(AdaptiveRoutes.BLOCK_LIST))
        assertEquals(2, routeSection(AdaptiveRoutes.MANUAL_BLOCK_LIST))
        assertEquals(2, routeSection(AdaptiveRoutes.ALLOWLIST))
        assertEquals(2, routeSection(AdaptiveRoutes.PATTERNS))
        assertEquals(2, routeSection(AdaptiveRoutes.COUNTRIES))
        assertEquals(2, routeSection(AdaptiveRoutes.SCHEDULES))
        assertEquals(2, routeSection(AdaptiveRoutes.ACTION_RULES))
    }

    @Test
    fun `settings and sub-screen routes map to index 3`() {
        assertEquals(3, routeSection(AdaptiveRoutes.SETTINGS))
        assertEquals(3, routeSection(AdaptiveRoutes.AUTO_RESPONDER))
        assertEquals(3, routeSection(AdaptiveRoutes.BACKUP))
        assertEquals(3, routeSection(AdaptiveRoutes.PRIVACY_POLICY))
        assertEquals(3, routeSection(AdaptiveRoutes.TERMS_CONDITIONS))
    }

    @Test
    fun `null route defaults to index 0`() {
        assertEquals(0, routeSection(null))
    }

    @Test
    fun `unknown route defaults to index 0`() {
        assertEquals(0, routeSection("unknown_route"))
        assertEquals(0, routeSection(""))
    }

    @Test
    fun `sectionRoutes list has four entries`() {
        assertEquals(4, sectionRoutes.size)
        assertEquals(AdaptiveRoutes.HOME, sectionRoutes[0])
        assertEquals(AdaptiveRoutes.BLOCK_LIST, sectionRoutes[2])
        assertEquals(AdaptiveRoutes.SETTINGS, sectionRoutes[3])
    }

    @Test
    fun `callLogRoute returns correct pattern`() {
        assertEquals("call_log/all", AdaptiveRoutes.callLogRoute())
        assertEquals("call_log/today", AdaptiveRoutes.callLogRoute("today"))
        assertEquals("call_log/week", AdaptiveRoutes.callLogRoute("week"))
    }
}

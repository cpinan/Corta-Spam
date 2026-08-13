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
    fun `keypad route maps to index 1`() {
        assertEquals(1, routeSection(AdaptiveRoutes.KEYPAD))
    }

    @Test
    fun `call log route maps to index 2`() {
        assertEquals(2, routeSection(AdaptiveRoutes.CALL_LOG))
        assertEquals(2, routeSection("call_log/all"))
        assertEquals(2, routeSection("call_log/today"))
        assertEquals(2, routeSection("call_log/week"))
    }

    @Test
    fun `block list hub and detail routes map to index 3`() {
        assertEquals(3, routeSection(AdaptiveRoutes.BLOCK_LIST))
        assertEquals(3, routeSection(AdaptiveRoutes.MANUAL_BLOCK_LIST))
        assertEquals(3, routeSection(AdaptiveRoutes.ALLOWLIST))
        assertEquals(3, routeSection(AdaptiveRoutes.PATTERNS))
        assertEquals(3, routeSection(AdaptiveRoutes.COUNTRIES))
        assertEquals(3, routeSection(AdaptiveRoutes.SCHEDULES))
        assertEquals(3, routeSection(AdaptiveRoutes.ACTION_RULES))
    }

    @Test
    fun `settings and sub-screen routes map to index 4`() {
        assertEquals(4, routeSection(AdaptiveRoutes.SETTINGS))
        assertEquals(4, routeSection(AdaptiveRoutes.AUTO_RESPONDER))
        assertEquals(4, routeSection(AdaptiveRoutes.BACKUP))
        assertEquals(4, routeSection(AdaptiveRoutes.PRIVACY_POLICY))
        assertEquals(4, routeSection(AdaptiveRoutes.TERMS_CONDITIONS))
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
    fun `sectionRoutes list has five entries`() {
        assertEquals(5, sectionRoutes.size)
        assertEquals(AdaptiveRoutes.HOME, sectionRoutes[0])
        assertEquals(AdaptiveRoutes.KEYPAD, sectionRoutes[1])
        assertEquals(AdaptiveRoutes.BLOCK_LIST, sectionRoutes[3])
        assertEquals(AdaptiveRoutes.SETTINGS, sectionRoutes[4])
    }

    /**
     * The nav bar builds its items from one list and highlights them from a separate `when`.
     * Inserting a tab shifts every index after it, and nothing else in the app would fail: the
     * bar would simply light up the wrong tab. This asserts the two agree for every section.
     */
    @Test
    fun `every section route selects its own tab`() {
        sectionRoutes.forEachIndexed { index, route ->
            assertEquals(index, routeSection(route), "route $route should select tab $index")
        }
    }

    @Test
    fun `callLogRoute returns correct pattern`() {
        assertEquals("call_log/all", AdaptiveRoutes.callLogRoute())
        assertEquals("call_log/today", AdaptiveRoutes.callLogRoute("today"))
        assertEquals("call_log/week", AdaptiveRoutes.callLogRoute("week"))
    }
}

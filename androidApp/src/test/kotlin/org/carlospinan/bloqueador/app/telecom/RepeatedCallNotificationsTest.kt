package org.carlospinan.bloqueador.app.telecom

import org.junit.Test
import kotlin.test.assertEquals

class RepeatedCallNotificationsTest {
    @Test
    fun `first call from a number counts one`() {
        val counter = RepeatedCallNotifications()
        assertEquals(1, counter.record("+34600123456").count)
    }

    @Test
    fun `repeat calls accumulate`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34600123456")
        counter.record("+34600123456")
        assertEquals(3, counter.record("+34600123456").count)
    }

    @Test
    fun `different numbers count separately`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34600123456")
        counter.record("+34600123456")
        assertEquals(1, counter.record("+34600999999").count)
        assertEquals(3, counter.record("+34600123456").count)
    }

    /**
     * The same caller reaching Telecom in two formats used to be two notifications, each claiming
     * to be the first. This is the same national-versus-international mismatch that has already
     * cost this app two shipped bugs, so the comparison is [org.carlospinan.bloqueador.app.rules.PhoneNumberParser.sameNumber]
     * rather than string equality.
     */
    @Test
    fun `a national and an international form are the same caller`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34611998877")
        assertEquals(2, counter.record("611 99 88 77").count)
    }

    @Test
    fun `the first form seen stays the notification identity`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34611998877")
        assertEquals("+34611998877", counter.record("611998877").representative)
        assertEquals("+34611998877", counter.representativeFor("611 99 88 77"))
    }

    /**
     * Two international numbers whose national parts match are different people. Merging them
     * would replace one caller's notification with another's — and its Block button with theirs.
     */
    @Test
    fun `identical national parts in different countries stay separate`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34611998877")
        assertEquals(1, counter.record("+51611998877").count)
    }

    @Test
    fun `clearing a number restarts its count`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34600123456")
        counter.record("+34600123456")
        counter.clear("+34600123456")
        assertEquals(1, counter.record("+34600123456").count)
    }

    @Test
    fun `clearing accepts any form of the number`() {
        val counter = RepeatedCallNotifications()
        counter.record("+34611998877")
        counter.record("+34611998877")
        counter.clear("611 99 88 77")
        assertEquals(1, counter.record("+34611998877").count)
    }

    @Test
    fun `an unknown number is its own representative`() {
        assertEquals("+34600123456", RepeatedCallNotifications().representativeFor("+34600123456"))
    }

    /** Runs for days inside a service, so the tracked set is bounded rather than a slow leak. */
    @Test
    fun `tracking is bounded and drops the oldest caller`() {
        val counter = RepeatedCallNotifications(maxTracked = 2)
        counter.record("+34600000001")
        counter.record("+34600000002")
        counter.record("+34600000003")
        assertEquals(1, counter.record("+34600000001").count)
        assertEquals(2, counter.record("+34600000003").count)
    }
}

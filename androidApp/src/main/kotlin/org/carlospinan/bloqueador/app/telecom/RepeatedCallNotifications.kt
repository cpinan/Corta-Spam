package org.carlospinan.bloqueador.app.telecom

import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

/**
 * Counts how many finished-call notifications a caller has produced since the user last dealt
 * with them, so a spammer calling five times leaves one notification saying five rather than five
 * notifications saying one.
 *
 * Callers are matched with [PhoneNumberParser.sameNumber], not with string equality and not with
 * a canonical key. That matters, and it is the reason this is a class rather than a `Map<String,
 * Int>`: `sameNumber` is deliberately asymmetric — a national number may match an international
 * one, while two *international* numbers with different country codes must never match even when
 * their national parts are identical. No single canonical string can express that, and reducing
 * each number to one has already caused two shipped bugs in this app's history.
 *
 * The first form of a number seen is kept as the caller's [Bucket.representative], so the
 * notification id stays stable even if Telecom hands over a different formatting next time.
 */
class RepeatedCallNotifications(
    private val maxTracked: Int = MAX_TRACKED,
) {
    data class Bucket(
        val representative: String,
        val count: Int,
    )

    private class Entry(
        val representative: String,
        var count: Int,
    )

    private val entries = ArrayDeque<Entry>()

    /**
     * Records one more notification for [number] and returns the caller's running total.
     *
     * Synchronized because the three call sites are coroutines on the service's scope and a
     * missed call can land while a blocked one is still being written.
     */
    fun record(number: String): Bucket =
        synchronized(entries) {
            val existing = entries.firstOrNull { PhoneNumberParser.sameNumber(it.representative, number) }
            if (existing != null) {
                existing.count += 1
                return Bucket(existing.representative, existing.count)
            }
            val entry = Entry(representative = number, count = 1)
            entries.addLast(entry)
            // Bounded on purpose: this lives in a service that can stay up for days, and an
            // unbounded map keyed by every stranger who ever called is a slow leak.
            while (entries.size > maxTracked) entries.removeFirst()
            Bucket(entry.representative, entry.count)
        }

    /** Forgets [number], so the next call from it starts counting at one again. */
    fun clear(number: String) {
        synchronized(entries) {
            entries.removeAll { PhoneNumberParser.sameNumber(it.representative, number) }
        }
    }

    /** The caller's stable identity for notification purposes, or [number] if it is unknown. */
    fun representativeFor(number: String): String =
        synchronized(entries) {
            entries.firstOrNull { PhoneNumberParser.sameNumber(it.representative, number) }?.representative ?: number
        }

    internal fun reset() {
        synchronized(entries) { entries.clear() }
    }

    companion object {
        private const val MAX_TRACKED = 64
    }
}

package org.carlospinan.bloqueador.app.telecom

/**
 * Which of the calls Telecom has handed over is on screen, and what happens to that when one ends.
 *
 * Generic over the call type purely so it can be tested. `android.telecom.Call` is final, has no
 * public constructor and ships no Robolectric shadow, so the only way to exercise this logic
 * against a real one is on a device with two live calls — which is precisely why the bug it exists
 * to fix survived: [InCallState] held one call in one field, a second call overwrote it, and when
 * that second call ended the state was cleared while the first was still connected. The in-call
 * activity finished, and the notification's Hang up action routed to a null field.
 *
 * [isRinging] is a function rather than a property so the caller reads live Telecom state at the
 * moment of the decision. A snapshot taken at `add` time would be stale by the time it matters.
 */
internal class CallStack<T : Any>(
    private val isRinging: (T) -> Boolean,
) {
    /** In arrival order. Identity comparison throughout: `Call` does not override `equals`. */
    private val calls = mutableListOf<T>()

    var primary: T? = null
        private set

    fun contains(call: T): Boolean = calls.any { it === call }

    /**
     * Adds [call] and puts it on screen. A newly arrived call always takes the screen — an
     * incoming one is the thing the user has to act on, and an outgoing one is the one they just
     * placed.
     *
     * Idempotent: Telecom can re-deliver a call, and adding it twice would inflate [otherCount]
     * and make the screen claim a call that is not there.
     */
    fun add(call: T) {
        if (!contains(call)) calls += call
        primary = call
    }

    /**
     * Removes [call], and returns what the screen should do about it.
     *
     * Promotion prefers a ringing call over one already in progress: a call the user has to answer
     * or decline is more urgent than one they are already on.
     */
    fun remove(call: T): Removal {
        val wasPrimary = primary === call
        calls.removeAll { it === call }
        if (!wasPrimary) return Removal.Unchanged
        val next = calls.firstOrNull { isRinging(it) } ?: calls.lastOrNull()
        primary = next
        return if (next == null) Removal.Empty else Removal.Promoted
    }

    /** How many calls are being held besides the one on screen. */
    fun otherCount(): Int = calls.count { it !== primary }

    /**
     * Forgets every call at once, for the case where they have all stopped being reachable
     * together — the owning `InCallService` being destroyed. Not the same as removing them one by
     * one: there is no promotion to do, because none of them is controllable any more.
     */
    fun clear() {
        calls.clear()
        primary = null
    }

    /**
     * An enum rather than a sealed type carrying the promoted call: the caller reads [primary]
     * for that, and a `Promoted<T>` nested in a generic class needs its own type parameter, which
     * forced an unchecked cast at the one place that used it.
     */
    enum class Removal {
        /** A background call ended; the screen keeps showing what it was showing. */
        Unchanged,

        /** The last call ended; there is nothing left to show. */
        Empty,

        /** The call on screen ended and another one takes its place — it is now [primary]. */
        Promoted,
    }
}

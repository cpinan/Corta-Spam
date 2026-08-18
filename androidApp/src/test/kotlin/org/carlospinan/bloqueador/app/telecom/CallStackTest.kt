package org.carlospinan.bloqueador.app.telecom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * The call-waiting bug, made reachable.
 *
 * [InCallState] held one `Call` in one field. A second call overwrote it, and when that second
 * call ended, `detach` saw the call it held was the one going away and cleared the state — so the
 * in-call activity finished while the *first* call was still connected, and the ongoing-call
 * notification's Hang up action routed to a null field and did nothing. The only way to end that
 * call was the system's own UI.
 *
 * None of that could be tested against `android.telecom.Call`: it is final, has no public
 * constructor, and ships no Robolectric shadow, which is exactly why the bug lasted. [CallStack]
 * exists so the decision is expressible over any type, and here it runs against a stand-in.
 */
class CallStackTest {
    /** A test call: identity is what [CallStack] compares, so these are deliberately not equal. */
    private class FakeCall(
        private val label: String,
        var ringing: Boolean = false,
    ) {
        override fun toString(): String = label
    }

    private fun stack() = CallStack<FakeCall> { it.ringing }

    @Test
    fun `the newest call takes the screen`() {
        val stack = stack()
        val first = FakeCall("first")
        val second = FakeCall("second")

        stack.add(first)
        stack.add(second)

        assertSame(second, stack.primary)
        assertEquals(1, stack.otherCount())
    }

    /**
     * The reported failure, in one test. Before the fix this left the screen with nothing to show
     * while `first` was still connected.
     */
    @Test
    fun `a waiting call ending gives the screen back to the call still in progress`() {
        val stack = stack()
        val first = FakeCall("first")
        val waiting = FakeCall("waiting", ringing = true)
        stack.add(first)
        stack.add(waiting)

        val removal = stack.remove(waiting)

        assertEquals(CallStack.Removal.Promoted, removal)
        assertSame(first, stack.primary)
        assertEquals(0, stack.otherCount())
    }

    @Test
    fun `the last call ending clears the screen`() {
        val stack = stack()
        val only = FakeCall("only")
        stack.add(only)

        assertEquals(CallStack.Removal.Empty, stack.remove(only))
        assertNull(stack.primary)
    }

    /** A background call ending must not disturb the call the user is looking at. */
    @Test
    fun `a background call ending leaves the screen alone`() {
        val stack = stack()
        val background = FakeCall("background")
        val shown = FakeCall("shown")
        stack.add(background)
        stack.add(shown)

        val removal = stack.remove(background)

        assertEquals(CallStack.Removal.Unchanged, removal)
        assertSame(shown, stack.primary)
        assertEquals(0, stack.otherCount())
    }

    /**
     * Promotion prefers a call that is ringing: one the user has to answer or decline outranks one
     * they are already on. Read live, not snapshotted at add time — a call is not ringing when it
     * arrives outgoing and may be by the time this decision is made.
     */
    @Test
    fun `a ringing call wins the promotion over an older connected one`() {
        val stack = stack()
        val connected = FakeCall("connected")
        val alsoRinging = FakeCall("alsoRinging")
        val shown = FakeCall("shown")
        stack.add(connected)
        stack.add(alsoRinging)
        stack.add(shown)
        alsoRinging.ringing = true

        stack.remove(shown)

        assertSame(alsoRinging, stack.primary)
    }

    /**
     * Telecom can hand the same call over twice. Counting it twice would make the screen claim a
     * second call that is not there, and leave a phantom behind when the real one ends.
     */
    @Test
    fun `adding the same call twice does not invent a second one`() {
        val stack = stack()
        val call = FakeCall("call")

        stack.add(call)
        stack.add(call)

        assertEquals(0, stack.otherCount())
        assertEquals(CallStack.Removal.Empty, stack.remove(call))
    }

    /**
     * The service being destroyed takes every call with it, and none of them gets promoted.
     *
     * Removing them one by one would promote the survivors, which is exactly wrong here: they are
     * all unreachable together, and putting one back on screen gave the user a call screen whose
     * hang-up button reached a dead binding.
     */
    @Test
    fun `clearing drops every call without promoting one`() {
        val stack = stack()
        val first = FakeCall("+34611998877")
        val second = FakeCall("+34600123456")
        stack.add(first)
        stack.add(second)

        stack.clear()

        assertNull(stack.primary)
        assertEquals(0, stack.otherCount())
        assertEquals(false, stack.contains(first))
        assertEquals(false, stack.contains(second))
    }

    /** Two distinct calls that happen to describe the same number are still two calls. */
    @Test
    fun `identity, not equality, decides which call is which`() {
        val stack = stack()
        val one = FakeCall("+34611998877")
        val twin = FakeCall("+34611998877")
        stack.add(one)
        stack.add(twin)

        stack.remove(twin)

        assertSame(one, stack.primary)
        assertEquals(0, stack.otherCount())
    }
}

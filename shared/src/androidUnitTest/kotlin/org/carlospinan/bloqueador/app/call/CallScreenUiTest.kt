package org.carlospinan.bloqueador.app.call

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/**
 * The call screen showed a bare number for every caller, including the ones already in the
 * user's address book — the app knew the name and put it on the notification, then dropped it on
 * the screen the user is actually looking at while the phone rings.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CallScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a named caller shows the name and keeps the number`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.RINGING,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
                displayName = "Ana Torres",
            )
        }

        composeTestRule.onNodeWithText("Ana Torres").assertExists()
        // Which of a contact's numbers is calling is a separate question from who is calling,
        // and a call screen is where both get asked.
        composeTestRule.onNodeWithText("+34611998877").assertExists()
    }

    @Test
    fun `an unknown caller still shows the number`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.RINGING,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
            )
        }

        composeTestRule.onNodeWithText("+34611998877").assertExists()
    }

    /** A withheld number has neither, and must not render an empty headline. */
    @Test
    fun `a withheld number falls back to the unknown label`() {
        composeTestRule.setContent {
            CallScreen(
                number = "",
                phase = CallUiPhase.RINGING,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
            )
        }

        composeTestRule.onNodeWithText("Unknown number").assertExists()
    }

    /**
     * Being the default phone app means owning the only call screen there is. Without a keypad on
     * it, "press 1 for accounts" is unreachable — every bank, airline and clinic menu ends at the
     * first prompt.
     */
    @Test
    fun `an active call can open the keypad and send a tone`() {
        val sent = StringBuilder()
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.ACTIVE,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
                onDtmf = { sent.append(it) },
            )
        }

        composeTestRule.onNodeWithText("Keypad").performClick()

        composeTestRule.onNodeWithText("1").performClick()
        // Scrolled to first: on a short screen the pad is taller than the space between the
        // caller's name and the hang-up button, and the bottom row is reached by scrolling the
        // pad rather than by pushing the hang-up button off the screen.
        composeTestRule.onNodeWithText("#").performScrollTo().performClick()

        assertEquals("1#", sent.toString())
    }

    /** Telecom drops a tone played on a call that has not connected, so the pad is not offered. */
    @Test
    fun `a ringing call offers no keypad`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.RINGING,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
            )
        }

        composeTestRule.onNodeWithText("Keypad").assertDoesNotExist()
    }

    /**
     * DTMF is fire-and-forget — nothing on the line echoes a digit back — so a user part-way
     * through a card number has no way to check what has already gone unless the screen keeps it.
     */
    @Test
    fun `tones already sent stay on screen`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.ACTIVE,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
                dtmfDigits = "4021",
            )
        }

        composeTestRule.onNodeWithText("4021").assertExists()
    }

    /**
     * With call waiting, showing one of two calls as though it were the only one is how someone
     * hangs up on the wrong person.
     */
    @Test
    fun `a second call in progress is said out loud`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.RINGING,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
                otherCallCount = 1,
            )
        }

        composeTestRule.onNodeWithText("You are already on another call").assertExists()
    }

    /** One call is the ordinary case and must stay silent about calls that do not exist. */
    @Test
    fun `a single call says nothing about other calls`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.ACTIVE,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
            )
        }

        composeTestRule.onNodeWithText("You are already on another call").assertDoesNotExist()
    }

    /** Hanging up must never be the button that scrolled away behind the pad. */
    @Test
    fun `the keypad can be closed again`() {
        composeTestRule.setContent {
            CallScreen(
                number = "+34611998877",
                phase = CallUiPhase.ACTIVE,
                onAnswer = {},
                onDecline = {},
                onHangUp = {},
            )
        }

        composeTestRule.onNodeWithText("Keypad").performClick()
        composeTestRule.onNodeWithText("5").assertExists()

        composeTestRule.onNodeWithText("Hide keypad").performClick()

        composeTestRule.onNodeWithText("5").assertDoesNotExist()
        composeTestRule.onNodeWithText("Hang up").assertExists()
    }
}

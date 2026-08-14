package org.carlospinan.bloqueador.app.call

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
}

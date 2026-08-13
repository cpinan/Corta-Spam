package org.carlospinan.bloqueador.app.keypad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class KeypadScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `typing digits builds the number`() {
        composeTestRule.setContent { KeypadScreen() }

        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        composeTestRule.onNodeWithText("600").assertExists()
    }

    @Test
    fun `call reports the typed number`() {
        var called: String? = null
        composeTestRule.setContent { KeypadScreen(onCall = { called = it }) }

        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("Call").performClick()

        assertEquals("12", called)
    }

    /** Without a '+' there is no way to dial an international number from this screen at all. */
    @Test
    fun `plus can be typed`() {
        var called: String? = null
        composeTestRule.setContent { KeypadScreen(onCall = { called = it }) }

        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("Call").performClick()

        assertEquals("+34", called)
    }

    @Test
    fun `delete removes the last digit`() {
        var called: String? = null
        composeTestRule.setContent { KeypadScreen(onCall = { called = it }) }

        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithText("8").performClick()
        composeTestRule.onNodeWithContentDescription("Delete last digit").performClick()
        composeTestRule.onNodeWithText("Call").performClick()

        assertEquals("7", called)
    }

    /** A Call button that places a call to nothing is a button that reports a bug as a call. */
    @Test
    fun `call is disabled until something is typed`() {
        composeTestRule.setContent { KeypadScreen() }

        composeTestRule.onNodeWithText("Call").assertIsNotEnabled()
        composeTestRule.onNodeWithText("9").performClick()
        composeTestRule.onNodeWithText("Call").assertIsEnabled()
    }

    @Test
    fun `an ACTION_DIAL number arrives pre-filled but is not dialled`() {
        var called: String? = null
        composeTestRule.setContent {
            KeypadScreen(
                dialRequest = DialRequest(number = "+34600123456", id = 1),
                onCall = { called = it },
            )
        }

        composeTestRule.onNodeWithText("+34600123456").assertExists()
        // ACTION_DIAL means "show it"; a tel: link on a web page must not place a call by itself.
        assertEquals(null, called)
    }

    /**
     * Tapping the same `tel:` link twice has to work the second time. Comparing requests by number
     * alone cannot tell "again" from "unchanged", which is why [DialRequest] carries an id.
     */
    @Test
    fun `a second request for the same number replaces what was typed`() {
        var request by mutableStateOf(DialRequest(number = "+34600123456", id = 1))
        var called: String? = null
        composeTestRule.setContent { KeypadScreen(dialRequest = request, onCall = { called = it }) }

        composeTestRule.onNodeWithContentDescription("Delete last digit").performClick()
        composeTestRule.onNodeWithText("+3460012345").assertExists()

        request = DialRequest(number = "+34600123456", id = 2)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Call").performClick()
        assertEquals("+34600123456", called)
    }

    /**
     * The same request recomposing is not a new one. Without the applied-id guard, returning to
     * the keypad tab would retype a number the user had deliberately deleted.
     */
    @Test
    fun `the same request is not applied twice`() {
        var request by mutableStateOf<DialRequest?>(DialRequest(number = "+34600123456", id = 1))
        var called: String? = null
        composeTestRule.setContent { KeypadScreen(dialRequest = request, onCall = { called = it }) }

        composeTestRule.onNodeWithContentDescription("Delete last digit").performClick()

        request = null
        composeTestRule.waitForIdle()
        request = DialRequest(number = "+34600123456", id = 1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Call").performClick()
        assertEquals("+3460012345", called)
    }
}

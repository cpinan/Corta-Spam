package org.carlospinan.bloqueador.app.keypad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.contacts.Contact
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
     * The search half of the screen. Without it, taking ROLE_DIALER left the user with no way to
     * call anyone whose number they could not type from memory.
     */
    @Test
    fun `typing a name shows the matching contact`() {
        composeTestRule.setContent {
            KeypadScreen(
                contacts =
                    listOf(
                        Contact(name = "Ana Torres", number = "+34 611 99 88 77"),
                        Contact(name = "Juana Ruiz", number = "600 111 222"),
                    ),
            )
        }

        composeTestRule.onNodeWithText("Ana Torres").assertDoesNotExist()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ana")

        composeTestRule.onNodeWithText("Ana Torres").assertExists()
        composeTestRule.onNodeWithText("Juana Ruiz").assertDoesNotExist()
    }

    /** Typed digits search the address book too -- the field is one input, not two. */
    @Test
    fun `typing digits on the pad finds a contact by number`() {
        composeTestRule.setContent {
            KeypadScreen(contacts = listOf(Contact(name = "Ana Torres", number = "+34 611 99 88 77")))
        }

        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("1").performClick()

        composeTestRule.onNodeWithText("Ana Torres").assertExists()
    }

    /**
     * Picking a contact fills the field rather than dialling, and the number it fills is the one
     * that gets called: a result that put the *name* in the dialler would call nobody.
     */
    @Test
    fun `picking a contact fills the number and calls it`() {
        var called: String? = null
        composeTestRule.setContent {
            KeypadScreen(
                onCall = { called = it },
                contacts = listOf(Contact(name = "Ana Torres", number = "+34 611 99 88 77")),
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ana")
        composeTestRule.onNodeWithText("Ana Torres").performClick()

        assertEquals(null, called)
        // The results list pushes the pad down: without the scroll the button is below the fold
        // and the tap lands on nothing, which is exactly what a user would see too.
        composeTestRule.onNodeWithText("Call").performScrollTo().performClick()
        assertEquals("+34 611 99 88 77", called)
    }

    /** An empty result set has to say so, or a missing contact reads as a broken search box. */
    @Test
    fun `a query with no match says so`() {
        composeTestRule.setContent {
            KeypadScreen(contacts = listOf(Contact(name = "Ana Torres", number = "+34611998877")))
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzz")

        composeTestRule.onNodeWithText("No contact matches what you typed.").assertExists()
    }

    /**
     * Without the permission the results area would just be permanently empty, which looks
     * exactly like an address book with nobody in it.
     */
    @Test
    fun `without the contacts permission the screen offers to ask for it`() {
        var asked = false
        composeTestRule.setContent {
            KeypadScreen(
                contactsPermissionGranted = false,
                onRequestContactsPermission = { asked = true },
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ana")

        composeTestRule.onNodeWithText("Grant contacts permission").performClick()
        assertTrue(asked)
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

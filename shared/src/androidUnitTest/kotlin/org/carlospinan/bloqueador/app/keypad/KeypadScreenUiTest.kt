package org.carlospinan.bloqueador.app.keypad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
// A realistic phone viewport rather than Robolectric's small default. These tests drive a
// scrolling screen by tapping keys, and on the default window the dial pad sat close enough to
// the fold that 132 dp of layout change pushed it out of reach -- which says nothing about the
// screen and everything about the window it was measured in.
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
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
     * Taking `ROLE_DIALER` took away the app the user used to save a number from, and this screen
     * shipped without a replacement: a number typed here, or read off a missed call, could be
     * dialled and never kept.
     */
    @Test
    fun `a number with no contact behind it can be saved as one`() {
        var added: String? = null
        composeTestRule.setContent { KeypadScreen(onAddContact = { added = it }) }

        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        composeTestRule.onNodeWithText("Create new contact").performScrollTo().performClick()

        assertEquals("600", added)
    }

    /**
     * Offered below the matches as well as instead of them: a typed number can look like a
     * contact's without being it, and the user still has to be able to save the one they typed.
     */
    @Test
    fun `saving is still offered when contacts already match`() {
        var added: String? = null
        composeTestRule.setContent {
            KeypadScreen(
                contacts = listOf(Contact(name = "Ana Torres", number = "+34600123456")),
                onAddContact = { added = it },
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ana")
        composeTestRule.onNodeWithText("Ana Torres").assertExists()

        composeTestRule.onNodeWithText("Create new contact").performScrollTo().performClick()

        assertEquals("Ana", added)
    }

    /** Nothing typed is nothing to save, and the empty screen is the pad, not a list of actions. */
    @Test
    fun `an empty keypad offers nothing to save`() {
        composeTestRule.setContent { KeypadScreen() }

        composeTestRule.onNodeWithText("Create new contact").assertDoesNotExist()
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

    /**
     * The dial pad must not move while the pad is being typed on.
     *
     * The regression this pins: the contact-match block sat between the field and the pad and
     * sized itself to its contents, so every keystroke that changed the number of matches moved
     * every key below it. Measured on a Pixel 8 Pro API 36 emulator against the release build,
     * typing a single `1` matched five contacts and pushed the `1` key 882 px down -- about a
     * third of the screen. The next digit landed wherever the pad had just moved to, and a tap
     * that missed the pad landed in the match list, whose rows replace the whole typed number.
     *
     * `7` is the probe because nothing here types it, so `onNodeWithText` cannot match the
     * number in the field instead of the key. The assertion is on the pad's own position rather
     * than on the match block's height: it is the pad moving that hurts, and a future layout that
     * keeps the pad still by some other means should be free to pass.
     */
    @Test
    fun `the dial pad stays put as contact matches come and go`() {
        val contacts =
            List(5) { index -> Contact(name = "Contact $index", number = "+3490012345$index") }
        composeTestRule.setContent { KeypadScreen(contacts = contacts) }

        val atRest = composeTestRule.onNodeWithText("7").getBoundsInRoot().top

        // Matches all five seeded contacts.
        composeTestRule.onNodeWithText("9").performClick()
        composeTestRule.waitForIdle()
        assertEquals(atRest, composeTestRule.onNodeWithText("7").getBoundsInRoot().top)

        // And back down to none, which used to move the pad the other way.
        composeTestRule.onNodeWithText("8").performClick()
        composeTestRule.waitForIdle()
        assertEquals(atRest, composeTestRule.onNodeWithText("7").getBoundsInRoot().top)
    }

    /**
     * Long-pressing delete clears the whole number, the way every phone's dialer does.
     *
     * Confirmed missing on a razr 50 ultra: a long press deleted exactly one digit, because the
     * control was a `TextButton`, which takes no `onLongClick`. Correcting a mistyped
     * international number therefore meant tapping delete once per digit.
     *
     * The assertion is on the resulting state -- the field is empty, so Call is disabled again --
     * rather than only on "600 is gone", which would also hold if the screen had cleared itself
     * for some entirely different reason.
     */
    @Test
    fun `long-pressing delete clears the whole number`() {
        composeTestRule.setContent { KeypadScreen() }

        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("600").assertExists()

        composeTestRule
            .onNodeWithContentDescription("Delete last digit")
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Call").assertIsNotEnabled()
    }

    /**
     * The results are a window of their own, so picking from them has to close them: a popup is
     * not in the screen's layout and nothing about the column changing will make it go away.
     *
     * The name has to be gone rather than merely the list: filling the field with the contact's
     * number re-matches that very contact, so a naive "show whenever the query matches" would
     * leave the results standing over the pad with the search already finished.
     */
    @Test
    fun `the floating results close once a contact is picked`() {
        composeTestRule.setContent {
            KeypadScreen(contacts = listOf(Contact(name = "Ana Torres", number = "+34 611 99 88 77")))
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ana")
        composeTestRule.onNodeWithText("Ana Torres").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ana Torres").assertDoesNotExist()
        composeTestRule.onNodeWithText("+34 611 99 88 77").assertExists()
    }

    /**
     * And come straight back when the query changes again. Dismissal is remembered against the
     * query it was dismissed for, not as a flag, so there is no state left set that a later
     * keystroke has to remember to clear.
     */
    @Test
    fun `the floating results return on the next keystroke`() {
        composeTestRule.setContent {
            KeypadScreen(
                contacts =
                    listOf(
                        Contact(name = "Ana Torres", number = "+34611998877"),
                        Contact(name = "Bea Ruiz", number = "+34600111222"),
                    ),
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ana")
        composeTestRule.onNodeWithText("Ana Torres").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ana Torres").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Delete last digit").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ana Torres").assertExists()
    }

    /**
     * The pad is pinned to the bottom of the window, which is what replaced the 132 dp of blank
     * space that used to be reserved between the field and the pad to keep the keys still. The
     * user read that band as a rendering fault, and on an empty query nothing is ever drawn in it.
     *
     * Asserting the Call button reaches the bottom of the window is the shape of the layout, not
     * its pixel values: a top-packed column leaves the button wherever the content above it ends,
     * which on this viewport is roughly the middle of the screen.
     */
    @Test
    fun `the pad and call button are anchored to the bottom of the window`() {
        composeTestRule.setContent { KeypadScreen() }

        val root = composeTestRule.onRoot().getBoundsInRoot()
        val callBottom = composeTestRule.onNodeWithText("Call").getBoundsInRoot().bottom

        assertTrue(
            callBottom > root.bottom - 80.dp,
            "Call ends at $callBottom, which is not near the bottom of $root",
        )
    }

    /**
     * The band between the title and the field is where the results open, and on a phone about
     * 1000 dp tall it is large -- twelve keys cannot fill it at any size that still reads as a
     * dial key. It holds the people most likely to be dialled from this screen rather than
     * nothing at all.
     */
    @Test
    fun `starred contacts are offered when nothing is typed`() {
        composeTestRule.setContent {
            KeypadScreen(
                contacts =
                    listOf(
                        Contact(name = "Ana Torres", number = "+34611998877", starred = true),
                        Contact(name = "Bea Ruiz", number = "600111222"),
                    ),
            )
        }

        composeTestRule.onNodeWithText("Favourites").assertExists()
        composeTestRule.onNodeWithText("Ana Torres").assertExists()
        composeTestRule.onNodeWithText("Bea Ruiz").assertDoesNotExist()
    }

    @Test
    fun `a phone with no starred contacts is offered no favourites`() {
        composeTestRule.setContent {
            KeypadScreen(contacts = listOf(Contact(name = "Bea Ruiz", number = "600111222")))
        }

        composeTestRule.onNodeWithText("Favourites").assertDoesNotExist()
    }

    /** Filling the field, not dialling: this strip sits under a thumb on its way to the keys. */
    @Test
    fun `picking a favourite fills the number instead of calling it`() {
        var called: String? = null
        composeTestRule.setContent {
            KeypadScreen(
                onCall = { called = it },
                contacts = listOf(Contact(name = "Ana Torres", number = "+34611998877", starred = true)),
            )
        }

        composeTestRule.onNodeWithText("Ana Torres").performClick()

        assertEquals(null, called)
        composeTestRule.onNodeWithText("Call").performClick()
        assertEquals("+34611998877", called)
    }

    /**
     * And they make way for the results, which occupy the same band. Both on screen at once would
     * be two lists of contacts stacked on top of each other.
     */
    @Test
    fun `favourites give up the band once something is typed`() {
        composeTestRule.setContent {
            KeypadScreen(
                contacts = listOf(Contact(name = "Ana Torres", number = "+34611998877", starred = true)),
            )
        }

        composeTestRule.onNodeWithText("Favourites").assertExists()

        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Favourites").assertDoesNotExist()
    }

    /**
     * Starring is a thing few people ever do, so the strip falls back to who rang recently rather
     * than leaving the band blank for them.
     */
    @Test
    fun `recent callers fill the strip when nothing is starred`() {
        composeTestRule.setContent {
            KeypadScreen(
                contacts = listOf(Contact(name = "Bea Ruiz", number = "600111222")),
                recent = listOf(Contact(name = "Ana Torres", number = "+34611998877")),
            )
        }

        composeTestRule.onNodeWithText("Recent").assertExists()
        composeTestRule.onNodeWithText("Ana Torres").assertExists()
    }

    /** Favourites outrank recents: they are a choice the user made, not one the phone made. */
    @Test
    fun `starred contacts win the strip over recent callers`() {
        composeTestRule.setContent {
            KeypadScreen(
                contacts = listOf(Contact(name = "Ana Torres", number = "+34611998877", starred = true)),
                recent = listOf(Contact(name = "Bea Ruiz", number = "600111222")),
            )
        }

        composeTestRule.onNodeWithText("Favourites").assertExists()
        composeTestRule.onNodeWithText("Recent").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bea Ruiz").assertDoesNotExist()
    }

    @Test
    fun `picking a recent caller fills the number`() {
        var called: String? = null
        composeTestRule.setContent {
            KeypadScreen(
                onCall = { called = it },
                recent = listOf(Contact(name = "Ana Torres", number = "+34611998877")),
            )
        }

        composeTestRule.onNodeWithText("Ana Torres").performClick()
        composeTestRule.onNodeWithText("Call").performClick()

        assertEquals("+34611998877", called)
    }

    /**
     * A new install has neither, and a band with nothing in it is the report this whole strip
     * exists to answer -- so it says what will appear there instead of saying nothing.
     */
    @Test
    fun `an empty strip says what it is for`() {
        composeTestRule.setContent { KeypadScreen() }

        composeTestRule
            .onNodeWithText("Your starred contacts and recent calls appear here.")
            .assertExists()
    }

    @Test
    fun `the strip hint gives way to the results once something is typed`() {
        composeTestRule.setContent { KeypadScreen() }

        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Your starred contacts and recent calls appear here.")
            .assertDoesNotExist()
    }

    /**
     * A recent caller who is not in the address book is labelled with their number, and the first
     * character of "+34902100200" is a plus sign. Every international number would then wear the
     * same badge.
     */
    @Test
    fun `a number badge shows its first digit rather than the plus`() {
        composeTestRule.setContent {
            KeypadScreen(recent = listOf(Contact(name = "+34902100200", number = "+34902100200")))
        }

        // Asserted on the badge itself rather than on "a 3 exists": the dial pad has one too.
        composeTestRule
            .onNodeWithContentDescription("+34902100200, +34902100200")
            .assertTextContains("3")
    }
}

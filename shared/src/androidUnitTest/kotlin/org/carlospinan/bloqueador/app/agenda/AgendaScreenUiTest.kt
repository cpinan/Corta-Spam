package org.carlospinan.bloqueador.app.agenda

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AgendaScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val ana = Contact(name = "Ana Torres", number = "+34611998877", starred = true)
    private val bea = Contact(name = "Bea Ruiz", number = "600111222")
    private val book = listOf(ana, bea)

    private fun blocked(number: String) = BlockedNumberEntry(id = 11, number = number, label = null, createdAt = 0)

    private fun allowed(number: String) = AllowlistedNumberEntry(id = 22, number = number, label = null, createdAt = 0)

    /**
     * Ana is expected twice and Bea once: she is starred, so she is in the favourites strip as
     * well as in the list below it -- the same person in the two places a phone shows favourites.
     */
    @Test
    fun `the address book is listed`() {
        composeTestRule.setContent { AgendaScreen(contacts = book) }

        composeTestRule.onAllNodesWithText("Ana Torres").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Bea Ruiz").assertCountEquals(1)
    }

    /** The platform's own favourites, across the top, the way every phone shows them. */
    @Test
    fun `starred contacts appear in the favourites strip`() {
        composeTestRule.setContent { AgendaScreen(contacts = book) }

        composeTestRule.onNodeWithText("Favourites").assertExists()
    }

    @Test
    fun `a phone with no starred contacts has no favourites strip`() {
        composeTestRule.setContent { AgendaScreen(contacts = listOf(bea)) }

        composeTestRule.onNodeWithText("Favourites").assertDoesNotExist()
    }

    @Test
    fun `the starred chip narrows the list to favourites`() {
        composeTestRule.setContent { AgendaScreen(contacts = book) }

        composeTestRule.onNodeWithText("Starred").performClick()

        composeTestRule.onNodeWithText("Ana Torres").assertExists()
        composeTestRule.onNodeWithText("Bea Ruiz").assertDoesNotExist()
    }

    /**
     * The filter the screen exists for: the block list holds numbers, so it cannot answer "which
     * of the people in my phone have I blocked".
     */
    @Test
    fun `the blocked chip narrows the list to blocked contacts`() {
        composeTestRule.setContent {
            AgendaScreen(contacts = book, blockedNumbers = listOf(blocked("600111222")))
        }

        composeTestRule.onNodeWithText("Blocked").performClick()

        composeTestRule.onNodeWithText("Bea Ruiz").assertExists()
        composeTestRule.onNodeWithText("Ana Torres").assertDoesNotExist()
    }

    @Test
    fun `the allowed chip narrows the list to allowlisted contacts`() {
        composeTestRule.setContent {
            AgendaScreen(contacts = book, allowlistedNumbers = listOf(allowed("+34611998877")))
        }

        composeTestRule.onNodeWithText("Allowed").performClick()

        composeTestRule.onNodeWithText("Ana Torres").assertExists()
        composeTestRule.onNodeWithText("Bea Ruiz").assertDoesNotExist()
    }

    @Test
    fun `the search box narrows the list`() {
        composeTestRule.setContent { AgendaScreen(contacts = book) }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Bea")

        composeTestRule.onNodeWithText("Bea Ruiz").assertExists()
        composeTestRule.onNodeWithText("Ana Torres").assertDoesNotExist()
    }

    /** A blocked contact says so on the row, not only once the dialog is open. */
    @Test
    fun `a blocked contact is marked on its own row`() {
        composeTestRule.setContent {
            AgendaScreen(contacts = listOf(bea), blockedNumbers = listOf(blocked("600111222")))
        }

        composeTestRule.onNodeWithText("On your block list").assertExists()
    }

    /**
     * Tapping a row opens the actions rather than dialling. A list being scrolled is the wrong
     * place to put a control that places a call on contact.
     */
    @Test
    fun `tapping a contact opens the actions instead of calling`() {
        var called: String? = null
        composeTestRule.setContent { AgendaScreen(contacts = book, onCallNumber = { called = it }) }

        composeTestRule.onNodeWithText("Bea Ruiz").performClick()

        composeTestRule.onNodeWithText("Block this number").assertExists()
        assertEquals(null, called)
    }

    @Test
    fun `the call action dials the contact`() {
        var called: String? = null
        composeTestRule.setContent { AgendaScreen(contacts = book, onCallNumber = { called = it }) }

        composeTestRule.onNodeWithText("Bea Ruiz").performClick()
        composeTestRule.onNodeWithText("Call").performClick()

        assertEquals("600111222", called)
    }

    @Test
    fun `the block action reports the contact number`() {
        var blockedNumber: String? = null
        composeTestRule.setContent { AgendaScreen(contacts = book, onBlockNumber = { blockedNumber = it }) }

        composeTestRule.onNodeWithText("Bea Ruiz").performClick()
        composeTestRule.onNodeWithText("Block this number").performClick()

        assertEquals("600111222", blockedNumber)
    }

    /**
     * One button that means one thing. Offering "Block this number" for a number that is already
     * blocked was a real bug in the call log: the tap did nothing visible, and nothing said the
     * number was already on the list.
     */
    @Test
    fun `an already blocked contact is offered unblock`() {
        var unblocked: Long? = null
        composeTestRule.setContent {
            AgendaScreen(
                contacts = listOf(bea),
                blockedNumbers = listOf(blocked("600111222")),
                onUnblockNumber = { unblocked = it },
            )
        }

        composeTestRule.onNodeWithText("Bea Ruiz").performClick()
        composeTestRule.onNodeWithText("Block this number").assertDoesNotExist()
        composeTestRule.onNodeWithText("Unblock this number").performClick()

        assertEquals(11L, unblocked)
    }

    /**
     * Without the permission the list would just be permanently empty, which looks exactly like a
     * phone with nobody in it.
     */
    @Test
    fun `without the contacts permission the screen offers to ask for it`() {
        var asked = false
        composeTestRule.setContent {
            AgendaScreen(contactsPermissionGranted = false, onRequestContactsPermission = { asked = true })
        }

        composeTestRule.onNodeWithText("Grant contacts permission").performClick()

        assertTrue(asked)
    }

    /**
     * An empty address book and an empty filter result are different problems, and the message for
     * one is nonsense advice for the other.
     */
    @Test
    fun `an empty filter result says so rather than claiming the phone has no contacts`() {
        composeTestRule.setContent { AgendaScreen(contacts = book) }

        composeTestRule.onNodeWithText("Blocked").performClick()

        composeTestRule.onNodeWithText("No contact matches this filter.").assertExists()
        composeTestRule.onNodeWithText("There are no contacts on this phone yet.").assertDoesNotExist()
    }

    @Test
    fun `an empty address book says the phone has no contacts`() {
        composeTestRule.setContent { AgendaScreen(contacts = emptyList()) }

        composeTestRule.onNodeWithText("There are no contacts on this phone yet.").assertExists()
    }
}

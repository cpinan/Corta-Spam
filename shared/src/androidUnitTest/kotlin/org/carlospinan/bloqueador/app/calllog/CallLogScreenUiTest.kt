package org.carlospinan.bloqueador.app.calllog

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CallDirection
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CallLogScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val blockedEntry =
        CallLogEntryData(
            id = 1L,
            number = "+34611223344",
            timestamp = 1234567890L,
            action = "BLOCKED",
            ruleType = "MANUAL",
            ruleId = 1L,
            ruleDetail = "Manually blocked",
        )

    private val allowedEntry =
        CallLogEntryData(
            id = 2L,
            number = "+34600987654",
            timestamp = 1234567890L,
            action = "ALLOWED",
            ruleType = null,
            ruleId = null,
            ruleDetail = null,
        )

    private val outgoingEntry =
        CallLogEntryData(
            id = 3L,
            number = "+34600111222",
            timestamp = 1234567890L,
            action = "ALLOWED",
            ruleType = null,
            ruleId = null,
            ruleDetail = null,
            direction = CallDirection.OUTGOING,
        )

    /**
     * An outgoing call is stored as ALLOWED because that is the only value the CHECK constraint
     * leaves, but it was never screened — labelling it "Allowed call" would report a decision
     * the app never made.
     */
    @Test
    fun `an outgoing call is labelled as outgoing, not allowed`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(outgoingEntry),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("+34600111222").assertExists()
        composeTestRule.onNodeWithText("Outgoing call", substring = true).assertExists()
        composeTestRule.onNodeWithText("Allowed call", substring = true).assertDoesNotExist()
    }

    @Test
    fun `blocked entry shows number and the localized status`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("+34611223344").assertExists()
        composeTestRule.onNodeWithText("Blocked call", substring = true).assertExists()
    }

    @Test
    fun `allowed entry shows number and the localized status`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(allowedEntry),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("+34600987654").assertExists()
        composeTestRule.onNodeWithText("Allowed call", substring = true).assertExists()
    }

    /**
     * A tapped notification has to land on that caller's actions. Opening the log and leaving the
     * user to find the row is the navigation the notification tap was supposed to replace.
     */
    @Test
    fun `a call-log request opens the actions for that caller`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry, allowedEntry),
                onBack = {},
                callLogRequest = CallLogRequest(number = "+34611223344", id = 1L),
            )
        }

        composeTestRule.onNodeWithText("Block this number").assertExists()
        composeTestRule.onNodeWithText("Call back").assertExists()
        composeTestRule.onNodeWithText("Copy number").assertExists()
    }

    @Test
    fun `the request's number is the one acted on`() {
        var blocked: String? = null
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry, allowedEntry),
                onBlockNumber = { blocked = it },
                onBack = {},
                callLogRequest = CallLogRequest(number = "+34600987654", id = 1L),
            )
        }

        composeTestRule.onNodeWithText("Block this number").performClick()

        assertEquals("+34600987654", blocked)
    }

    /** Dismissed is dismissed: a recomposition must not put the dialog back on screen. */
    @Test
    fun `a dismissed request does not reopen`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                onBack = {},
                callLogRequest = CallLogRequest(number = "+34611223344", id = 1L),
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Block this number").assertDoesNotExist()
    }

    /** The chips have to filter the list that is on screen, not merely render. */
    @Test
    fun `the outgoing chip hides incoming calls`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry, outgoingEntry),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Outgoing").performClick()

        composeTestRule.onNodeWithText("+34600111222").assertExists()
        composeTestRule.onNodeWithText("+34611223344").assertDoesNotExist()
    }

    @Test
    fun `searching narrows the log to the matching number`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry, allowedEntry),
                onBack = {},
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("600987")

        composeTestRule.onNodeWithText("+34600987654").assertExists()
        composeTestRule.onNodeWithText("+34611223344").assertDoesNotExist()
    }

    /**
     * "No calls yet" is advice for an empty log; for an empty *filter result* it is wrong, and it
     * sends the user looking for a bug in the screening instead of in their own filter.
     */
    @Test
    fun `a filter that matches nothing says so, not that the log is empty`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                onBack = {},
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzz")

        composeTestRule.onNodeWithText("No calls match this filter.").assertExists()
    }

    @Test
    fun `a date chip asks the ViewModel rather than filtering on screen`() {
        var requested: String? = null
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                onBack = {},
                onSelectTimeFilter = { requested = it },
            )
        }

        composeTestRule.onNodeWithText("This week").performClick()

        assertEquals("week", requested)
    }

    // --- Rule state on the row and in the tap dialog -------------------------------------------
    //
    // The log used to show only what happened to each call, and to offer Block/Allow regardless
    // of what the user's rules already said. Blocking an already-blocked caller looked identical
    // to a broken button.

    @Test
    fun `a row for a number on the block list says so`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                blockedNumbers = listOf(BlockedNumberEntry(4L, blockedEntry.number, null, 0)),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("On your block list").assertExists()
    }

    @Test
    fun `a row for a number on the allowlist says so`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(allowedEntry),
                allowlistedNumbers = listOf(AllowlistedNumberEntry(5L, allowedEntry.number, null, 0)),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("On your allowlist").assertExists()
    }

    /**
     * A call that was blocked by a rule the user has since deleted. The outcome label stays —
     * that call really was blocked — but the number carries no badge and the action on offer is
     * Block, not Unblock.
     */
    @Test
    fun `a blocked call whose number is no longer on any list carries no badge`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Blocked call").assertExists()
        composeTestRule.onNodeWithText("On your block list").assertDoesNotExist()
    }

    @Test
    fun `tapping a blocked number offers unblock and returns the rule id`() {
        var unblocked: Long? = null
        var blockedAgain: String? = null
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                blockedNumbers = listOf(BlockedNumberEntry(42L, blockedEntry.number, null, 0)),
                onUnblockNumber = { unblocked = it },
                onBlockNumber = { blockedAgain = it },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(blockedEntry.number).performClick()
        composeTestRule.onNodeWithText("Unblock this number").performClick()

        assertEquals(42L, unblocked)
        assertEquals(null, blockedAgain)
    }

    @Test
    fun `tapping a number that is not blocked still offers block`() {
        var blocked: String? = null
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(allowedEntry),
                onBlockNumber = { blocked = it },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(allowedEntry.number).performClick()
        composeTestRule.onNodeWithText("Unblock this number").assertDoesNotExist()
        composeTestRule.onNodeWithText("Block this number").performClick()

        assertEquals(allowedEntry.number, blocked)
    }

    @Test
    fun `tapping an allowlisted number offers removal from the allowlist`() {
        var removed: Long? = null
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(allowedEntry),
                allowlistedNumbers = listOf(AllowlistedNumberEntry(13L, allowedEntry.number, null, 0)),
                onRemoveFromAllowlist = { removed = it },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(allowedEntry.number).performClick()
        composeTestRule.onNodeWithText("Remove from allowlist").performClick()

        assertEquals(13L, removed)
    }

    /**
     * The rule was saved the way the user dials it and the call arrived in E.164. Matching the
     * strings would have offered Block for a number that is already blocked.
     */
    @Test
    fun `a rule saved nationally is recognised for an E164 call`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                blockedNumbers = listOf(BlockedNumberEntry(1L, "611 22 33 44", null, 0)),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("On your block list").assertExists()
    }

    @Test
    fun `empty state shows title`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = emptyList(),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("Call Log").assertExists()
    }
}

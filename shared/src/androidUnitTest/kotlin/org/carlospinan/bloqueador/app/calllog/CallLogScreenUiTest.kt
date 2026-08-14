package org.carlospinan.bloqueador.app.calllog

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

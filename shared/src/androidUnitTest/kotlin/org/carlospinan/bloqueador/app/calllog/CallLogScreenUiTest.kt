package org.carlospinan.bloqueador.app.calllog

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
    fun `blocked entry shows number and action`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(blockedEntry),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("+34611223344").assertExists()
        composeTestRule.onNodeWithText("BLOCKED").assertExists()
    }

    @Test
    fun `allowed entry shows number and action`() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(allowedEntry),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("+34600987654").assertExists()
        composeTestRule.onNodeWithText("ALLOWED").assertExists()
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

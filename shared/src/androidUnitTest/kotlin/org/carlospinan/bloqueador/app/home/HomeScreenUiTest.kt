package org.carlospinan.bloqueador.app.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomeScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `blocked today count displayed`() {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(blockedToday = 12, blockedThisWeek = 25, blockedThisMonth = 45),
                blockingEnabled = true,
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = {},
                onNavigateToStats = {},
                onToggleBlocking = {},
            )
        }
        composeTestRule.onNodeWithText("12").assertExists()
    }

    @Test
    fun `quick grid items visible`() {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(),
                blockingEnabled = true,
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = {},
                onNavigateToStats = {},
                onToggleBlocking = {},
            )
        }
        composeTestRule.onNodeWithText("View call log").assertExists()
        composeTestRule.onNodeWithText("View stats").assertExists()
        composeTestRule.onNodeWithText("Manage block lists").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun `title shows Corta Spam`() {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(),
                blockingEnabled = true,
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = {},
                onNavigateToStats = {},
                onToggleBlocking = {},
            )
        }
        composeTestRule.onNodeWithText("Corta Spam").assertExists()
    }
}

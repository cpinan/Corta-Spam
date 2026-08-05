package org.carlospinan.bloqueador.app.blocklist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class BlockListHubUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `all 6 hub cards render with counts`() {
        composeTestRule.setContent {
            BlockListHubScreen(
                blockedCount = 5,
                allowlistedCount = 3,
                patternCount = 2,
                countryCount = 1,
                scheduleCount = 4,
                actionCount = 6,
                onNavigateToManual = {},
                onNavigateToAllowlist = {},
                onNavigateToPatterns = {},
                onNavigateToCountries = {},
                onNavigateToSchedules = {},
                onNavigateToActionRules = {},
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("Blocked Numbers").assertExists()
        composeTestRule.onNodeWithText("Allowlist").assertExists()
        composeTestRule.onNodeWithText("Pattern Rules").assertExists()
        composeTestRule.onNodeWithText("Country Rules").assertExists()
        composeTestRule.onNodeWithText("Quiet Hours").assertExists()
        composeTestRule.onNodeWithText("Repeat callers").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun `zero counts still render`() {
        composeTestRule.setContent {
            BlockListHubScreen(
                blockedCount = 0,
                allowlistedCount = 0,
                patternCount = 0,
                countryCount = 0,
                scheduleCount = 0,
                actionCount = 0,
                onNavigateToManual = {},
                onNavigateToAllowlist = {},
                onNavigateToPatterns = {},
                onNavigateToCountries = {},
                onNavigateToSchedules = {},
                onNavigateToActionRules = {},
                onBack = {},
            )
        }
        composeTestRule.onAllNodesWithText("0").assertCountEquals(6)
    }

    @Test
    fun `title visible`() {
        composeTestRule.setContent {
            BlockListHubScreen(
                blockedCount = 0,
                allowlistedCount = 0,
                patternCount = 0,
                countryCount = 0,
                scheduleCount = 0,
                actionCount = 0,
                onNavigateToManual = {},
                onNavigateToAllowlist = {},
                onNavigateToPatterns = {},
                onNavigateToCountries = {},
                onNavigateToSchedules = {},
                onNavigateToActionRules = {},
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("Block Lists").assertExists()
    }
}

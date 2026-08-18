package org.carlospinan.bloqueador.app.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.adaptive.AdaptiveScaffold
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomeScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setHome(
        callInProgress: Boolean = false,
        onReturnToCall: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(blockingEnabled = true),
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToCallLogReview = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = {},
                onNavigateToStats = {},
                onToggleBlocking = {},
                callInProgress = callInProgress,
                onReturnToCall = onReturnToCall,
            )
        }
    }

    /**
     * Back on the call screen backgrounds it rather than ending it, and Home does the same. The
     * ongoing-call notification is one way back; it is gone entirely when the user has switched
     * notifications off, so this card is the other.
     */
    @Test
    fun `a live call offers a way back to it`() {
        var returned = false
        setHome(callInProgress = true, onReturnToCall = { returned = true })

        composeTestRule.onNodeWithText("A call is in progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Return to call").performClick()

        assertTrue(returned)
    }

    @Test
    fun `no call means no card`() {
        setHome(callInProgress = false)

        composeTestRule.onNodeWithText("A call is in progress").assertDoesNotExist()
    }

    @Test
    fun `blocked today count displayed`() {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(blockedToday = 12, blockedThisWeek = 25, blockedThisMonth = 45, blockingEnabled = true),
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToCallLogReview = {},
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
                state = HomeUiState(blockingEnabled = true),
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToCallLogReview = {},
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
                state = HomeUiState(blockingEnabled = true),
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToCallLogReview = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = {},
                onNavigateToStats = {},
                onToggleBlocking = {},
            )
        }
        composeTestRule.onNodeWithText("Corta Spam").assertExists()
    }

    /**
     * Every other test here asserts `assertExists`, which passes for a node that was composed and
     * then clipped off the bottom of the window -- exactly what a user reports as "the screen does
     * not scroll". This one shrinks the window until Home overflows, proves the last quick link is
     * genuinely off-screen, and then requires that scrolling reaches it.
     */
    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h320dp")
    fun `last quick link is off screen but reachable by scrolling`() {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(blockingEnabled = true),
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToCallLogReview = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = {},
                onNavigateToStats = {},
                onToggleBlocking = {},
            )
        }

        composeTestRule.onNodeWithText("Settings").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Settings").performScrollTo().assertIsDisplayed()
    }

    /**
     * The same assertion through the shape the app actually renders: Home is never shown bare, it
     * sits inside [AdaptiveScaffold], whose NavigationBar takes the bottom of the window away from
     * it. A screen that scrolls on its own and stops scrolling once something is docked under it is
     * the failure a bare-screen test cannot see.
     */
    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h320dp")
    fun `last quick link is reachable inside the navigation scaffold`() {
        composeTestRule.setContent {
            AdaptiveScaffold(
                windowSizeClass = WindowSizeClass.Compact,
                selectedIndex = 0,
                onNavigate = {},
            ) {
                HomeScreen(
                    state = HomeUiState(blockingEnabled = true),
                    onNavigateToCallLog = {},
                    onNavigateToCallLogToday = {},
                    onNavigateToCallLogThisWeek = {},
                    onNavigateToCallLogThisMonth = {},
                    onNavigateToCallLogReview = {},
                    onNavigateToBlockList = {},
                    onNavigateToSettings = {},
                    onNavigateToStats = {},
                    onToggleBlocking = {},
                )
            }
        }

        // "Settings" is also a NavigationBar label, so pick the one inside the scrolling area.
        val settingsQuickLink =
            composeTestRule.onNode(hasText("Settings") and hasAnyAncestor(hasScrollAction()))

        settingsQuickLink.assertIsNotDisplayed()
        settingsQuickLink.performScrollTo().assertIsDisplayed()
    }
}

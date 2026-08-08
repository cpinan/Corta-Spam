package org.carlospinan.bloqueador.app.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

/**
 * Home is where a user notices that the app has stopped working. These warnings previously
 * existed only on the Settings screen, so an app that had lost the dialer role looked identical
 * on Home to one that was screening every call.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomePermissionWarningsUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `losing the dialer role is visible on home, not only in settings`() {
        setHome(dialerRoleHeld = false)

        composeTestRule.onNodeWithText("Not your default phone app").assertExists()
        composeTestRule.onNodeWithText("Call screening is off", substring = true).assertExists()
    }

    @Test
    fun `the dialer role banner re-requests the role`() {
        var requested = false
        setHome(dialerRoleHeld = false, onRequestDialerRole = { requested = true })

        composeTestRule.onNodeWithText("Set as default").performClick()

        assertTrue(requested)
    }

    @Test
    fun `a single missing permission surfaces on home`() {
        setHome(notificationsPermissionGranted = false)

        composeTestRule.onNodeWithText("Notifications are off").assertExists()
        composeTestRule.onNodeWithText("More permissions need attention").assertDoesNotExist()
    }

    @Test
    fun `a fully permitted app shows no warnings at all`() {
        setHome()

        composeTestRule.onNodeWithText("Not your default phone app").assertDoesNotExist()
        composeTestRule.onNodeWithText("Notifications are off").assertDoesNotExist()
        composeTestRule.onNodeWithText("Full-screen alerts are off").assertDoesNotExist()
        composeTestRule.onNodeWithText("Phone permission needed").assertDoesNotExist()
        composeTestRule.onNodeWithText("More permissions need attention").assertDoesNotExist()
    }

    /**
     * Observed on a razr 50 ultra after a fresh install where nothing was granted: four stacked
     * error cards pushed the blocking toggle and every counter below the fold. Home shows the
     * one that breaks the most and points at Settings for the rest.
     */
    @Test
    fun `home shows only the most severe warning and points at settings for the rest`() {
        setHome(
            dialerRoleHeld = false,
            notificationsPermissionGranted = false,
            callPhonePermissionGranted = false,
        )

        composeTestRule.onNodeWithText("Not your default phone app").assertExists()
        composeTestRule.onNodeWithText("Notifications are off").assertDoesNotExist()
        composeTestRule.onNodeWithText("Phone permission needed").assertDoesNotExist()
        composeTestRule.onNodeWithText("More permissions need attention").assertExists()
    }

    @Test
    fun `the see-all row navigates to settings`() {
        var navigated = false
        setHome(
            dialerRoleHeld = false,
            notificationsPermissionGranted = false,
            onNavigateToSettings = { navigated = true },
        )

        composeTestRule.onNodeWithText("More permissions need attention").performClick()

        assertTrue(navigated)
    }

    /**
     * The contradiction this exists for: the banner said screening was off while the caption
     * directly below it said "spam and blocked calls are filtered". Without the dialer role no
     * call reaches the app at all, so the second statement was simply false, and it is the one
     * a user reads to decide whether they are protected.
     */
    @Test
    fun `the toggle caption does not claim calls are filtered without the dialer role`() {
        setHome(blockingEnabled = true, dialerRoleHeld = false)

        composeTestRule
            .onNodeWithText("spam and blocked calls are filtered", substring = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("no calls reach Corta Spam", substring = true).assertExists()
    }

    @Test
    fun `the toggle caption is unchanged once the role is held`() {
        setHome(blockingEnabled = true, dialerRoleHeld = true)

        composeTestRule
            .onNodeWithText("spam and blocked calls are filtered", substring = true)
            .assertExists()
    }

    @Test
    fun `blocking switched off reads the same either way`() {
        setHome(blockingEnabled = false, dialerRoleHeld = false)

        composeTestRule.onNodeWithText("all calls are allowed through", substring = true).assertExists()
        composeTestRule.onNodeWithText("no calls reach Corta Spam", substring = true).assertDoesNotExist()
    }

    private fun setHome(
        blockingEnabled: Boolean = true,
        dialerRoleHeld: Boolean = true,
        notificationsPermissionGranted: Boolean = true,
        fullScreenIntentAllowed: Boolean = true,
        callPhonePermissionGranted: Boolean = true,
        onRequestDialerRole: () -> Unit = {},
        onNavigateToSettings: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HomeScreen(
                state = HomeUiState(blockingEnabled = blockingEnabled),
                onNavigateToCallLog = {},
                onNavigateToCallLogToday = {},
                onNavigateToCallLogThisWeek = {},
                onNavigateToCallLogThisMonth = {},
                onNavigateToCallLogReview = {},
                onNavigateToBlockList = {},
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToStats = {},
                onToggleBlocking = {},
                dialerRoleHeld = dialerRoleHeld,
                notificationsPermissionGranted = notificationsPermissionGranted,
                fullScreenIntentAllowed = fullScreenIntentAllowed,
                callPhonePermissionGranted = callPhonePermissionGranted,
                onRequestDialerRole = onRequestDialerRole,
            )
        }
    }
}

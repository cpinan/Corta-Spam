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
    fun `missing notifications and call permission surface on home`() {
        setHome(notificationsPermissionGranted = false, callPhonePermissionGranted = false)

        composeTestRule.onNodeWithText("Notifications are off").assertExists()
        composeTestRule.onNodeWithText("Phone permission needed").assertExists()
    }

    @Test
    fun `a fully permitted app shows no warnings at all`() {
        setHome()

        composeTestRule.onNodeWithText("Not your default phone app").assertDoesNotExist()
        composeTestRule.onNodeWithText("Notifications are off").assertDoesNotExist()
        composeTestRule.onNodeWithText("Full-screen alerts are off").assertDoesNotExist()
        composeTestRule.onNodeWithText("Phone permission needed").assertDoesNotExist()
    }

    private fun setHome(
        dialerRoleHeld: Boolean = true,
        notificationsPermissionGranted: Boolean = true,
        fullScreenIntentAllowed: Boolean = true,
        callPhonePermissionGranted: Boolean = true,
        onRequestDialerRole: () -> Unit = {},
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
                dialerRoleHeld = dialerRoleHeld,
                notificationsPermissionGranted = notificationsPermissionGranted,
                fullScreenIntentAllowed = fullScreenIntentAllowed,
                callPhonePermissionGranted = callPhonePermissionGranted,
                onRequestDialerRole = onRequestDialerRole,
            )
        }
    }
}

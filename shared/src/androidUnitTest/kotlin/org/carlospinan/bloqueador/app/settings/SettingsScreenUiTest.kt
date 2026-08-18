package org.carlospinan.bloqueador.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        windowSizeClass: WindowSizeClass,
        state: SettingsUiState = SettingsUiState(blockingEnabled = true, autoAllowContacts = false),
        showGrantContacts: Boolean = false,
        notificationsPermissionGranted: Boolean = true,
        onNavigateToAutoResponder: () -> Unit = {},
        appVersion: AppVersion? = null,
    ) {
        composeTestRule.setContent {
            SettingsScreen(
                state = state,
                showGrantContacts = showGrantContacts,
                notificationsPermissionGranted = notificationsPermissionGranted,
                onSetBlockingEnabled = {},
                onSetAutoAllowContacts = {},
                onSetDefaultAction = {},
                onSetSpamEnabled = {},
                onRequestContactsPermission = {},
                onNavigateToAutoResponder = onNavigateToAutoResponder,
                onNavigateToBackup = {},
                appVersion = appVersion,
                onBack = {},
                windowSizeClass = windowSizeClass,
            )
        }
    }

    /**
     * A bug report that does not say which build it is about costs a round trip to find out, and
     * the app had no screen anywhere that answered the question.
     */
    @Test
    fun `compact settings shows the running version and build number`() {
        setContent(
            windowSizeClass = WindowSizeClass.Compact,
            appVersion = AppVersion(name = "1.4.0", code = 6),
        )

        composeTestRule.onNodeWithText("Version").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("1.4.0 (6)").assertExists()
    }

    /** The tablet layout files it under About, where the rest of the app's identity already is. */
    @Test
    fun `expanded settings shows the version in the About section`() {
        setContent(
            windowSizeClass = WindowSizeClass.Expanded,
            appVersion = AppVersion(name = "1.4.0", code = 6),
        )

        composeTestRule.onNodeWithText("1.4.0 (6)").assertDoesNotExist()

        composeTestRule.onNodeWithText("About").performClick()

        composeTestRule.onNodeWithText("1.4.0 (6)").assertExists()
    }

    @Test
    fun `all items render`() {
        composeTestRule.setContent {
            SettingsScreen(
                state = SettingsUiState(blockingEnabled = true, autoAllowContacts = false),
                showGrantContacts = false,
                onSetBlockingEnabled = {},
                onSetAutoAllowContacts = {},
                onSetDefaultAction = {},
                onSetSpamEnabled = {},
                onRequestContactsPermission = {},
                onNavigateToAutoResponder = {},
                onNavigateToBackup = {},
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("Settings").assertExists()
        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
        composeTestRule.onNodeWithText("Terms & Conditions").assertExists()
    }

    @Test
    fun `privacy and terms items have correct labels`() {
        composeTestRule.setContent {
            SettingsScreen(
                state = SettingsUiState(blockingEnabled = true, autoAllowContacts = false),
                showGrantContacts = false,
                onSetBlockingEnabled = {},
                onSetAutoAllowContacts = {},
                onSetDefaultAction = {},
                onSetSpamEnabled = {},
                onRequestContactsPermission = {},
                onNavigateToAutoResponder = {},
                onNavigateToBackup = {},
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("How your data is handled").assertExists()
        composeTestRule.onNodeWithText("Open source license and usage terms").assertExists()
    }

    // Expanded means >=840dp; Robolectric otherwise renders a phone-width window, where the
    // 340dp list pane starves the detail pane and nothing in it is actually displayed.
    @Test
    @Config(sdk = [34], qualifiers = "w1280dp-h800dp")
    fun `expanded shows the section list beside a detail pane`() {
        setContent(WindowSizeClass.Expanded)

        // Left pane: every section, plus the two rows that navigate away rather than
        // becoming sections of their own.
        composeTestRule.onNodeWithText("Blocking").assertExists()
        composeTestRule.onNodeWithText("Contacts").assertExists()
        composeTestRule.onNodeWithText("Notifications").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
        composeTestRule.onNodeWithText("Auto-responder (Experimental)").assertExists()
        // Not asserting on the Backup row's title: SettingDropdown renders settings_backup as
        // both its title and its value, so the same text matches two nodes.

        // Detail pane opens on Blocking.
        composeTestRule.onNodeWithText("Default action").assertExists()
    }

    // Expanded means >=840dp; Robolectric otherwise renders a phone-width window, where the
    // 340dp list pane starves the detail pane and nothing in it is actually displayed.
    @Test
    @Config(sdk = [34], qualifiers = "w1280dp-h800dp")
    fun `expanded swaps the detail pane when another section is selected`() {
        setContent(WindowSizeClass.Expanded)
        composeTestRule.onNodeWithText("Default action").assertExists()

        composeTestRule.onNodeWithText("About").performClick()

        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
        // Blocking's controls are gone -- this is a swap, not an accumulating column.
        composeTestRule.onNodeWithText("Default action").assertDoesNotExist()
    }

    // Expanded means >=840dp; Robolectric otherwise renders a phone-width window, where the
    // 340dp list pane starves the detail pane and nothing in it is actually displayed.
    @Test
    @Config(sdk = [34], qualifiers = "w1280dp-h800dp")
    fun `expanded keeps permission warnings visible on every section`() {
        setContent(WindowSizeClass.Expanded, notificationsPermissionGranted = false)

        // Warning is app-level, so it must not be filed away behind the Notifications
        // section where a user browsing Blocking would never see it.
        composeTestRule.onNodeWithText("Notifications are off").assertIsDisplayed()

        composeTestRule.onNodeWithText("About").performClick()

        composeTestRule.onNodeWithText("Notifications are off").assertIsDisplayed()
    }

    @Test
    fun `compact keeps every setting in one column`() {
        setContent(WindowSizeClass.Compact)

        // No section picker; the flat list is unchanged from before the split layout.
        composeTestRule.onNodeWithText("Default action").assertExists()
        composeTestRule.onNodeWithText("Auto-allow contacts").assertExists()
        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
    }
}

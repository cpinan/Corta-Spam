package org.carlospinan.bloqueador.app.settings

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
class SettingsScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `all items render`() {
        composeTestRule.setContent {
            SettingsScreen(
                blockingEnabled = true,
                autoAllowContacts = false,
                defaultAction = DefaultAction.ALLOW,
                spamEnabled = false,
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
                blockingEnabled = true,
                autoAllowContacts = false,
                defaultAction = DefaultAction.ALLOW,
                spamEnabled = false,
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
}

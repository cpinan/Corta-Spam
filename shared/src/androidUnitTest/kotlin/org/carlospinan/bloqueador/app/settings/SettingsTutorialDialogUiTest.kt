package org.carlospinan.bloqueador.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsTutorialDialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `explains the default action and says block is the default`() {
        composeTestRule.setContent { SettingsTutorialDialog(onOpenSettings = {}, onDismiss = {}) }

        composeTestRule.onNodeWithText("Welcome", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Default action", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Block is the default", substring = true).assertIsDisplayed()
    }

    @Test
    fun `open settings reports the open and not a plain dismissal`() {
        var opened = 0
        var dismissed = 0
        composeTestRule.setContent {
            SettingsTutorialDialog(onOpenSettings = { opened++ }, onDismiss = { dismissed++ })
        }

        composeTestRule.onNodeWithText("Open Settings").performClick()

        assertEquals(1, opened)
        assertEquals(0, dismissed)
    }

    @Test
    fun `got it dismisses without opening settings`() {
        var opened = 0
        var dismissed = 0
        composeTestRule.setContent {
            SettingsTutorialDialog(onOpenSettings = { opened++ }, onDismiss = { dismissed++ })
        }

        composeTestRule.onNodeWithText("Got it").performClick()

        assertEquals(0, opened)
        assertEquals(1, dismissed)
    }
}

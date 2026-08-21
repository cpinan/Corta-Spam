package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class AdaptiveScaffoldUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `compact shows navigation bar with every section`() {
        composeTestRule.setContent {
            AdaptiveScaffold(
                windowSizeClass = WindowSizeClass.Compact,
                selectedIndex = 0,
                onNavigate = {},
            ) {
                Text("Content")
            }
        }
        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Log").assertExists()
        composeTestRule.onNodeWithText("Lists").assertExists()
        composeTestRule.onNodeWithText("Contacts").assertExists()
        // Settings is reached from Home's own card, not from the bar: Material's bar tops out at
        // five items and the address book of a dialer replacement earns the slot.
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithText("Content").assertExists()
    }

    @Test
    fun `medium shows navigation rail with every section`() {
        composeTestRule.setContent {
            AdaptiveScaffold(
                windowSizeClass = WindowSizeClass.Medium,
                selectedIndex = 0,
                onNavigate = {},
            ) {
                Text("Content")
            }
        }
        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Log").assertExists()
        composeTestRule.onNodeWithText("Lists").assertExists()
        composeTestRule.onNodeWithText("Contacts").assertExists()
        // Settings is reached from Home's own card, not from the bar: Material's bar tops out at
        // five items and the address book of a dialer replacement earns the slot.
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun `expanded shows navigation rail with labels`() {
        composeTestRule.setContent {
            AdaptiveScaffold(
                windowSizeClass = WindowSizeClass.Expanded,
                selectedIndex = 0,
                onNavigate = {},
            ) {
                Text("Content")
            }
        }
        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Log").assertExists()
    }
}

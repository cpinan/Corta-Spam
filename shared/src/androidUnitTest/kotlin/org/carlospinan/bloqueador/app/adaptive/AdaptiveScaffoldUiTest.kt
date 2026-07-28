package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.material3.Text
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
class AdaptiveScaffoldUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `compact shows navigation bar with 4 items`() {
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
        composeTestRule.onNodeWithText("Settings").assertExists()
        composeTestRule.onNodeWithText("Content").assertExists()
    }

    @Test
    fun `medium shows navigation rail with 4 items`() {
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
        composeTestRule.onNodeWithText("Settings").assertExists()
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

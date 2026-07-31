package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
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
class AdaptiveContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `compact renders content`() {
        composeTestRule.setContent {
            AdaptiveContent(windowSizeClass = WindowSizeClass.Compact) {
                Text("Hello Compact")
            }
        }
        composeTestRule.onNodeWithText("Hello Compact").assertExists()
    }

    @Test
    fun `medium renders content`() {
        composeTestRule.setContent {
            AdaptiveContent(windowSizeClass = WindowSizeClass.Medium) {
                Text("Hello Medium")
            }
        }
        composeTestRule.onNodeWithText("Hello Medium").assertExists()
    }

    @Test
    fun `scrollable content renders`() {
        composeTestRule.setContent {
            AdaptiveContent(
                windowSizeClass = WindowSizeClass.Compact,
                contentModifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text("Item 1")
                Text("Item 2")
                Text("Item 3")
            }
        }
        composeTestRule.onNodeWithText("Item 1").assertExists()
        composeTestRule.onNodeWithText("Item 3").assertExists()
    }
}

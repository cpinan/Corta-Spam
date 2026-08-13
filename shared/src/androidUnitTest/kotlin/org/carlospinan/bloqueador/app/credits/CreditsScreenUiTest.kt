package org.carlospinan.bloqueador.app.credits

import androidx.compose.ui.test.assertIsDisplayed
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
class CreditsScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `says names are on the way while the list is empty`() {
        composeTestRule.setContent {
            CreditsScreen(contributors = emptyList(), onBack = {})
        }

        composeTestRule
            .onNodeWithText("Names are on the way", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `lists a contributor and their contribution`() {
        // Guards the state the screen will spend most of its life in but has never rendered:
        // an empty-only screen is the shape a placeholder can hide a broken list behind.
        composeTestRule.setContent {
            CreditsScreen(
                contributors = listOf(Contributor(name = "Ada Lovelace", contribution = "Reported the ringtone bug")),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reported the ringtone bug").assertIsDisplayed()
    }

    @Test
    fun `renders a contributor who has no contribution line`() {
        composeTestRule.setContent {
            CreditsScreen(contributors = listOf(Contributor(name = "Grace Hopper")), onBack = {})
        }

        composeTestRule.onNodeWithText("Grace Hopper").assertIsDisplayed()
    }
}

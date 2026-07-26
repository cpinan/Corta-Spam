package org.carlospinan.bloqueador.app.onboarding

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private class FakeScreenTestGateway(
    var isDefault: Boolean,
) : DefaultDialerGateway {
    override fun isDefaultDialer(): Boolean = isDefault
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DialerOnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notRequestedShowsPermissionExplainer() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false))

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Continue").assertExists()
        composeTestRule.onNodeWithText("Not now").assertExists()
    }

    @Test
    fun requestingShowsIndicator() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false))
        viewModel.onRequestStarted()

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Waiting for the system dialog…").assertExists()
    }

    @Test
    fun deniedShowsRetryScreen() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false))
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = false)

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Try again").assertExists()
        composeTestRule.onNodeWithText("Continue without it").assertExists()
    }

    @Test
    fun grantedSkipsToContent() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false))
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = true)

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = { Text("home") })
        }

        composeTestRule.onNodeWithText("home").assertExists()
    }

    @Test
    fun alreadyDefaultSkipsToContent() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = true))

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = { Text("home") })
        }

        composeTestRule.onNodeWithText("home").assertExists()
    }
}

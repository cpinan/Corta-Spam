package org.carlospinan.bloqueador.app.onboarding

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.testing.FakeSettingsRepository
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
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeSettingsRepository())

        val state = viewModel.state.value

        composeTestRule.setContent {
            DialerOnboardingScreen(state = state, onIntent = viewModel::onIntent, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Continue").assertExists()
        composeTestRule.onNodeWithText("Not now").assertExists()
    }

    @Test
    fun requestingShowsIndicator() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)

        val state = viewModel.state.value

        composeTestRule.setContent {
            DialerOnboardingScreen(state = state, onIntent = viewModel::onIntent, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Waiting for the system dialog…").assertExists()
    }

    @Test
    fun deniedShowsRetryScreen() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = false))

        val state = viewModel.state.value

        composeTestRule.setContent {
            DialerOnboardingScreen(state = state, onIntent = viewModel::onIntent, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Try again").assertExists()
        composeTestRule.onNodeWithText("Continue without it").assertExists()
    }

    @Test
    fun grantedSkipsToContent() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = true))

        val state = viewModel.state.value

        composeTestRule.setContent {
            DialerOnboardingScreen(
                state = state,
                onIntent = viewModel::onIntent,
                onRequestRole = {},
                content = { Text("home") },
            )
        }

        composeTestRule.onNodeWithText("home").assertExists()
    }

    @Test
    fun alreadyDefaultSkipsToContent() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = true), FakeSettingsRepository())

        val state = viewModel.state.value

        composeTestRule.setContent {
            DialerOnboardingScreen(
                state = state,
                onIntent = viewModel::onIntent,
                onRequestRole = {},
                content = { Text("home") },
            )
        }

        composeTestRule.onNodeWithText("home").assertExists()
    }
}

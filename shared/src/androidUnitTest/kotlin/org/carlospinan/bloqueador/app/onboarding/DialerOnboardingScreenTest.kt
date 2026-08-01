package org.carlospinan.bloqueador.app.onboarding

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.carlospinan.bloqueador.app.settings.DefaultAction
import org.carlospinan.bloqueador.app.settings.SettingsRepository
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

private class FakeScreenTestSettingsRepository : SettingsRepository {
    override val blockingEnabled = MutableStateFlow(true)
    override val autoAllowContacts = MutableStateFlow(true)
    override val defaultAction = MutableStateFlow(DefaultAction.ALLOW)
    override val notificationsEnabled = MutableStateFlow(true)
    override val welcomeShown = false

    override suspend fun setBlockingEnabled(enabled: Boolean) {}

    override suspend fun setAutoAllowContacts(enabled: Boolean) {}

    override suspend fun setDefaultAction(action: DefaultAction) {}

    override suspend fun setNotificationsEnabled(enabled: Boolean) {}

    override suspend fun setWelcomeShown() {}
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DialerOnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notRequestedShowsPermissionExplainer() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeScreenTestSettingsRepository())

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Continue").assertExists()
        composeTestRule.onNodeWithText("Not now").assertExists()
    }

    @Test
    fun requestingShowsIndicator() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeScreenTestSettingsRepository())
        viewModel.onRequestStarted()

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = {})
        }

        composeTestRule.onNodeWithText("Waiting for the system dialog…").assertExists()
    }

    @Test
    fun deniedShowsRetryScreen() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeScreenTestSettingsRepository())
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
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = false), FakeScreenTestSettingsRepository())
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = true)

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = { Text("home") })
        }

        composeTestRule.onNodeWithText("home").assertExists()
    }

    @Test
    fun alreadyDefaultSkipsToContent() {
        val viewModel = DialerOnboardingViewModel(FakeScreenTestGateway(isDefault = true), FakeScreenTestSettingsRepository())

        composeTestRule.setContent {
            DialerOnboardingScreen(viewModel = viewModel, onRequestRole = {}, content = { Text("home") })
        }

        composeTestRule.onNodeWithText("home").assertExists()
    }
}

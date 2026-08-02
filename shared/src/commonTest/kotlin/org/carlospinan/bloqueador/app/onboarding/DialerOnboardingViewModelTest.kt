package org.carlospinan.bloqueador.app.onboarding

import kotlinx.coroutines.flow.MutableStateFlow
import org.carlospinan.bloqueador.app.settings.DefaultAction
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeGateway(
    var isDefault: Boolean,
) : DefaultDialerGateway {
    override fun isDefaultDialer(): Boolean = isDefault
}

private class FakeSettingsRepository(
    override val welcomeShown: Boolean = false,
) : SettingsRepository {
    override val blockingEnabled = MutableStateFlow(true)
    override val autoAllowContacts = MutableStateFlow(true)
    override val defaultAction = MutableStateFlow(DefaultAction.ALLOW)
    override val notificationsEnabled = MutableStateFlow(true)
    override val repeatedCallerBypassCount = MutableStateFlow(0)

    override suspend fun setBlockingEnabled(enabled: Boolean) {}

    override suspend fun setAutoAllowContacts(enabled: Boolean) {}

    override suspend fun setDefaultAction(action: DefaultAction) {}

    override suspend fun setNotificationsEnabled(enabled: Boolean) {}

    override suspend fun setRepeatedCallerBypassCount(count: Int) {}

    override suspend fun setWelcomeShown() {}
}

class DialerOnboardingViewModelTest {
    @Test
    fun startsNotRequestedWhenNotDefault() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        assertEquals(DialerOnboardingState.NOT_REQUESTED, viewModel.state.value)
    }

    @Test
    fun startsAlreadyDefaultWhenAlreadyDefault() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = true), FakeSettingsRepository())
        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value)
    }

    @Test
    fun continueMovesToRequesting() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onRequestStarted()
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value)
    }

    @Test
    fun grantedResultMovesToGranted() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = true)
        assertEquals(DialerOnboardingState.GRANTED, viewModel.state.value)
    }

    @Test
    fun deniedResultAllowsRetry() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = false)
        assertEquals(DialerOnboardingState.DENIED, viewModel.state.value)

        viewModel.onRequestStarted()
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value)
    }

    @Test
    fun resultOutsideRequestingThrows() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        assertFailsWith<IllegalStateException> { viewModel.onRequestResult(granted = true) }
    }

    @Test
    fun refreshPicksUpExternalGrantWhileNotRequested() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())

        gateway.isDefault = true
        viewModel.refresh()

        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value)
    }

    @Test
    fun refreshIsNoOpWhileRequesting() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())
        viewModel.onRequestStarted()

        gateway.isDefault = false
        viewModel.refresh()

        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value)
    }

    @Test
    fun welcomeShownReflectsRepositoryInitialValue() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository(welcomeShown = true))
        assertEquals(true, viewModel.welcomeShown.value)
    }
}

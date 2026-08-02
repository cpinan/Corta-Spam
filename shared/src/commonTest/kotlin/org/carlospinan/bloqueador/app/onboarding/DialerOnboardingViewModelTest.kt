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
        assertEquals(DialerOnboardingState.NOT_REQUESTED, viewModel.state.value.dialerState)
    }

    @Test
    fun startsAlreadyDefaultWhenAlreadyDefault() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = true), FakeSettingsRepository())
        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value.dialerState)
    }

    @Test
    fun continueMovesToRequesting() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value.dialerState)
    }

    @Test
    fun grantedResultMovesToGranted() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = true))
        assertEquals(DialerOnboardingState.GRANTED, viewModel.state.value.dialerState)
    }

    @Test
    fun deniedResultAllowsRetry() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = false))
        assertEquals(DialerOnboardingState.DENIED, viewModel.state.value.dialerState)

        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value.dialerState)
    }

    @Test
    fun resultOutsideRequestingThrows() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
        assertFailsWith<IllegalStateException> {
            viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = true))
        }
    }

    @Test
    fun refreshPicksUpExternalGrantWhileNotRequested() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())

        gateway.isDefault = true
        viewModel.onIntent(DialerOnboardingIntent.Refresh)

        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value.dialerState)
    }

    @Test
    fun refreshIsNoOpWhileRequesting() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)

        gateway.isDefault = false
        viewModel.onIntent(DialerOnboardingIntent.Refresh)

        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value.dialerState)
    }

    @Test
    fun welcomeShownReflectsRepositoryInitialValue() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository(welcomeShown = true))
        assertEquals(true, viewModel.state.value.welcomeShown)
    }
}

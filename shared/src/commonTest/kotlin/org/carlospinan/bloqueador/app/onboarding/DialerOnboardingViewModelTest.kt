package org.carlospinan.bloqueador.app.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeGateway(var isDefault: Boolean) : DefaultDialerGateway {
    override fun isDefaultDialer(): Boolean = isDefault
}

class DialerOnboardingViewModelTest {

    @Test
    fun startsNotRequestedWhenNotDefault() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false))
        assertEquals(DialerOnboardingState.NOT_REQUESTED, viewModel.state.value)
    }

    @Test
    fun startsAlreadyDefaultWhenAlreadyDefault() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = true))
        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value)
    }

    @Test
    fun continueMovesToRequesting() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false))
        viewModel.onRequestStarted()
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value)
    }

    @Test
    fun grantedResultMovesToGranted() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false))
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = true)
        assertEquals(DialerOnboardingState.GRANTED, viewModel.state.value)
    }

    @Test
    fun deniedResultAllowsRetry() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false))
        viewModel.onRequestStarted()
        viewModel.onRequestResult(granted = false)
        assertEquals(DialerOnboardingState.DENIED, viewModel.state.value)

        viewModel.onRequestStarted()
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value)
    }

    @Test
    fun resultOutsideRequestingThrows() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false))
        assertFailsWith<IllegalStateException> { viewModel.onRequestResult(granted = true) }
    }

    @Test
    fun refreshPicksUpExternalGrantWhileNotRequested() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway)

        gateway.isDefault = true
        viewModel.refresh()

        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value)
    }

    @Test
    fun refreshIsNoOpWhileRequesting() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway)
        viewModel.onRequestStarted()

        gateway.isDefault = false
        viewModel.refresh()

        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value)
    }
}

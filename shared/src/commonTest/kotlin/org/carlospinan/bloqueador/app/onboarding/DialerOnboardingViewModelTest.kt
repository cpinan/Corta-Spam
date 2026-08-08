package org.carlospinan.bloqueador.app.onboarding

import org.carlospinan.bloqueador.app.testing.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeGateway(
    var isDefault: Boolean,
) : DefaultDialerGateway {
    override fun isDefaultDialer(): Boolean = isDefault
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

    /**
     * The regression this exists for: refresh only ever upgraded, so a user who revoked the
     * dialer role in system Settings kept an app that reported [DialerOnboardingState.GRANTED]
     * and screened nothing. Positive assertion on purpose -- the state must land on
     * NOT_REQUESTED, not merely "stop being GRANTED".
     */
    @Test
    fun refreshDowngradesWhenTheRoleIsRevokedExternally() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = true))
        assertEquals(DialerOnboardingState.GRANTED, viewModel.state.value.dialerState)

        gateway.isDefault = false
        viewModel.onIntent(DialerOnboardingIntent.Refresh)

        assertEquals(DialerOnboardingState.NOT_REQUESTED, viewModel.state.value.dialerState)
        assertFalse(viewModel.state.value.dialerRoleHeld)
    }

    @Test
    fun refreshDowngradesFromAlreadyDefaultToo() {
        val gateway = FakeGateway(isDefault = true)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())
        assertEquals(DialerOnboardingState.ALREADY_DEFAULT, viewModel.state.value.dialerState)

        gateway.isDefault = false
        viewModel.onIntent(DialerOnboardingIntent.Refresh)

        assertEquals(DialerOnboardingState.NOT_REQUESTED, viewModel.state.value.dialerState)
    }

    @Test
    fun refreshLeavesDeniedAloneSoTheRetryScreenStays() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)
        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = false))

        viewModel.onIntent(DialerOnboardingIntent.Refresh)

        assertEquals(DialerOnboardingState.DENIED, viewModel.state.value.dialerState)
    }

    /**
     * A refresh landing between the role dialog opening and its result must not move the state,
     * or [DialerOnboardingIntent.RequestResult]'s `check` throws on a perfectly normal grant.
     */
    @Test
    fun refreshWhileRequestingDoesNotBreakTheIncomingResult() {
        val gateway = FakeGateway(isDefault = false)
        val viewModel = DialerOnboardingViewModel(gateway, FakeSettingsRepository())
        viewModel.onIntent(DialerOnboardingIntent.RequestStarted)

        gateway.isDefault = true
        viewModel.onIntent(DialerOnboardingIntent.Refresh)
        assertEquals(DialerOnboardingState.REQUESTING, viewModel.state.value.dialerState)

        viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = true))
        assertEquals(DialerOnboardingState.GRANTED, viewModel.state.value.dialerState)
    }

    @Test
    fun dialerRoleHeldIsTrueOnlyWhileTheRoleIsActuallyHeld() {
        assertTrue(DialerOnboardingUiState(dialerState = DialerOnboardingState.GRANTED).dialerRoleHeld)
        assertTrue(DialerOnboardingUiState(dialerState = DialerOnboardingState.ALREADY_DEFAULT).dialerRoleHeld)
        assertFalse(DialerOnboardingUiState(dialerState = DialerOnboardingState.NOT_REQUESTED).dialerRoleHeld)
        assertFalse(DialerOnboardingUiState(dialerState = DialerOnboardingState.DENIED).dialerRoleHeld)
        assertFalse(DialerOnboardingUiState(dialerState = DialerOnboardingState.REQUESTING).dialerRoleHeld)
    }

    @Test
    fun welcomeShownReflectsRepositoryInitialValue() {
        val viewModel = DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository(welcomeShown = true))
        assertEquals(true, viewModel.state.value.welcomeShown)
    }

    @Test
    fun permissionsPromptShownReflectsRepositoryInitialValue() {
        assertFalse(
            DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository())
                .state.value.permissionsPromptShown,
        )
        assertTrue(
            DialerOnboardingViewModel(FakeGateway(isDefault = false), FakeSettingsRepository(permissionsPromptShown = true))
                .state.value.permissionsPromptShown,
        )
    }
}

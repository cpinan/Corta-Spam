package org.carlospinan.bloqueador.app.onboarding

/**
 * State machine for the Android default-dialer onboarding flow (docs/MILESTONES.md M1).
 * Not applicable on iOS -- there is no default-phone-app concept there (docs/SPEC.md §1).
 */
enum class DialerOnboardingState {
    NOT_REQUESTED,
    ALREADY_DEFAULT,
    REQUESTING,
    GRANTED,
    DENIED,
}

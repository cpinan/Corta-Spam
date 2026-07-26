package org.carlospinan.bloqueador.app.di

import org.koin.core.module.Module

/** Each platform binds its own concrete implementations (DriverFactory, and on Android, the dialer-onboarding gateway/view-model). */
expect fun platformModule(): Module

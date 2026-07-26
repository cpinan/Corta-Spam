package org.carlospinan.bloqueador.app.onboarding

/** Platform check for whether this app currently holds the default-dialer role. */
interface DefaultDialerGateway {
    fun isDefaultDialer(): Boolean
}

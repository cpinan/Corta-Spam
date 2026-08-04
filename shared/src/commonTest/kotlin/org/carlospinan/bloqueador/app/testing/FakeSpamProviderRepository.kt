package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository

/**
 * In-memory [SpamProviderRepository] for tests. One flag, write-through setter.
 */
internal class FakeSpamProviderRepository(
    override val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false),
) : SpamProviderRepository {
    override suspend fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}

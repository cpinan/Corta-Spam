package org.carlospinan.bloqueador.app.spam

import kotlinx.coroutines.flow.Flow

interface SpamProviderRepository {
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}

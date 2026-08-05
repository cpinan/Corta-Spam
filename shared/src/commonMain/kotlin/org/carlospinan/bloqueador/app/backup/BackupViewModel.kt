package org.carlospinan.bloqueador.app.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.RuleRepository

/**
 * Outcomes of a backup action.
 *
 * These carry data, not sentences. They used to hold pre-built English strings ("Exported
 * successfully.", "Imported 12 rules (3 blocked, ...)") assembled here, which every user saw in
 * English no matter which of the app's four locales they were running. Formatting happens in
 * the Composable, where `stringResource` is available.
 */
sealed interface BackupEffect {
    data class Exported(
        val json: String,
    ) : BackupEffect

    data object ExportSucceeded : BackupEffect

    data class ImportSucceeded(
        val result: ImportResult,
    ) : BackupEffect

    /** [cause] is an exception message — developer-facing detail, shown as-is after a localized prefix. */
    data class ExportFailed(
        val cause: String?,
    ) : BackupEffect

    data class ImportFailed(
        val cause: String?,
    ) : BackupEffect
}

sealed interface BackupIntent {
    data object Export : BackupIntent

    data class Import(
        val json: String,
    ) : BackupIntent
}

class BackupViewModel(
    private val ruleRepository: RuleRepository,
) : ViewModel() {
    private val _effect = Channel<BackupEffect>()
    val effect: Flow<BackupEffect> = _effect.receiveAsFlow()

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            BackupIntent.Export -> exportJson()
            is BackupIntent.Import -> importJson(intent.json)
        }
    }

    private fun exportJson() {
        viewModelScope.launch {
            try {
                val json = ruleRepository.exportAll()
                _effect.send(BackupEffect.Exported(json))
                _effect.send(BackupEffect.ExportSucceeded)
            } catch (e: Exception) {
                _effect.send(BackupEffect.ExportFailed(e.message))
            }
        }
    }

    private fun importJson(json: String) {
        viewModelScope.launch {
            try {
                _effect.send(BackupEffect.ImportSucceeded(ruleRepository.importAll(json)))
            } catch (e: Exception) {
                _effect.send(BackupEffect.ImportFailed(e.message))
            }
        }
    }
}

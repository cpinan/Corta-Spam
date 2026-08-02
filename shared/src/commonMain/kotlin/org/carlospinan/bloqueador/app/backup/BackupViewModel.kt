package org.carlospinan.bloqueador.app.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.RuleRepository

sealed interface BackupEffect {
    data class Exported(
        val json: String,
    ) : BackupEffect

    data class Success(
        val message: String,
    ) : BackupEffect

    data class Failure(
        val message: String,
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
                _effect.send(BackupEffect.Success("Exported successfully."))
            } catch (e: Exception) {
                _effect.send(BackupEffect.Failure("Export failed: ${e.message}"))
            }
        }
    }

    private fun importJson(json: String) {
        viewModelScope.launch {
            try {
                val result = ruleRepository.importAll(json)
                _effect.send(
                    BackupEffect.Success(
                        "Imported ${result.total} rules " +
                            "(${result.blockedNumbersImported} blocked, " +
                            "${result.allowlistedNumbersImported} allowlisted, " +
                            "${result.patternsImported} patterns, " +
                            "${result.countriesImported} countries, " +
                            "${result.actionsImported} actions, " +
                            "${result.schedulesImported} schedules).",
                    ),
                )
            } catch (e: Exception) {
                _effect.send(BackupEffect.Failure("Import failed: ${e.message}"))
            }
        }
    }
}

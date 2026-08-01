package org.carlospinan.bloqueador.app.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.RuleRepository

sealed interface BackupEffect {
    data class Success(
        val message: String,
    ) : BackupEffect

    data class Failure(
        val message: String,
    ) : BackupEffect
}

class BackupViewModel(
    private val ruleRepository: RuleRepository,
) : ViewModel() {
    private val _effect = Channel<BackupEffect>()
    val effect: Flow<BackupEffect> = _effect.receiveAsFlow()

    fun exportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = ruleRepository.exportAll()
                onResult(json)
                _effect.send(BackupEffect.Success("Exported successfully."))
            } catch (e: Exception) {
                _effect.send(BackupEffect.Failure("Export failed: ${e.message}"))
            }
        }
    }

    fun importJson(json: String) {
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

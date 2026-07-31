package org.carlospinan.bloqueador.app.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.RuleRepository

data class BackupUiState(
    val message: String? = null,
    val isError: Boolean = false,
)

class BackupViewModel(
    private val ruleRepository: RuleRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun exportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = ruleRepository.exportAll()
                onResult(json)
                _state.value = BackupUiState(message = "Exported successfully.")
            } catch (e: Exception) {
                _state.value = BackupUiState(message = "Export failed: ${e.message}", isError = true)
            }
        }
    }

    fun importJson(json: String) {
        viewModelScope.launch {
            try {
                val result = ruleRepository.importAll(json)
                _state.value =
                    BackupUiState(
                        message =
                            "Imported ${result.total} rules " +
                                "(${result.blockedNumbersImported} blocked, " +
                                "${result.allowlistedNumbersImported} allowlisted, " +
                                "${result.patternsImported} patterns, " +
                                "${result.countriesImported} countries, " +
                                "${result.actionsImported} actions, " +
                                "${result.schedulesImported} schedules).",
                    )
            } catch (e: Exception) {
                _state.value = BackupUiState(message = "Import failed: ${e.message}", isError = true)
            }
        }
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            try {
                val numbers =
                    csv
                        .lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                var count = 0
                for (number in numbers) {
                    ruleRepository.addBlockedNumber(number)
                    count++
                }
                _state.value = BackupUiState(message = "Imported $count numbers from CSV.")
            } catch (e: Exception) {
                _state.value = BackupUiState(message = "CSV import failed: ${e.message}", isError = true)
            }
        }
    }

    fun clearMessage() {
        _state.value = BackupUiState()
    }
}

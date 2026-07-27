package org.carlospinan.bloqueador.app.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.RuleRepository

class BlockListViewModel(
    private val ruleRepository: RuleRepository,
) : ViewModel() {
    val blockedNumbers: StateFlow<List<BlockedNumberEntry>> =
        ruleRepository
            .blockedNumbers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowlistedNumbers: StateFlow<List<AllowlistedNumberEntry>> =
        ruleRepository
            .allowlistedNumbers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    private val _allowlistedCount = MutableStateFlow(0)
    val allowlistedCount: StateFlow<Int> = _allowlistedCount.asStateFlow()

    init {
        viewModelScope.launch {
            blockedNumbers.collect { _blockedCount.value = it.size }
        }
        viewModelScope.launch {
            allowlistedNumbers.collect { _allowlistedCount.value = it.size }
        }
    }

    fun addBlockedNumber(number: String) {
        viewModelScope.launch {
            ruleRepository.addBlockedNumber(number)
        }
    }

    fun removeBlockedNumber(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeBlockedNumber(id)
        }
    }

    fun addAllowlistedNumber(number: String) {
        viewModelScope.launch {
            ruleRepository.addAllowlistedNumber(number)
        }
    }

    fun removeAllowlistedNumber(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeAllowlistedNumber(id)
        }
    }
}

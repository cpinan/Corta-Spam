package org.carlospinan.bloqueador.app.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry

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

    val patternRules: StateFlow<List<PatternRuleEntry>> =
        ruleRepository
            .patternRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countryRules: StateFlow<List<CountryRuleEntry>> =
        ruleRepository
            .countryRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionRules: StateFlow<List<ActionRuleEntry>> =
        ruleRepository
            .actionRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleRules: StateFlow<List<ScheduleRuleEntry>> =
        ruleRepository
            .scheduleRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    private val _allowlistedCount = MutableStateFlow(0)
    val allowlistedCount: StateFlow<Int> = _allowlistedCount.asStateFlow()

    private val _patternCount = MutableStateFlow(0)
    val patternCount: StateFlow<Int> = _patternCount.asStateFlow()

    private val _countryCount = MutableStateFlow(0)
    val countryCount: StateFlow<Int> = _countryCount.asStateFlow()

    private val _actionCount = MutableStateFlow(0)
    val actionCount: StateFlow<Int> = _actionCount.asStateFlow()

    private val _scheduleCount = MutableStateFlow(0)
    val scheduleCount: StateFlow<Int> = _scheduleCount.asStateFlow()

    init {
        viewModelScope.launch {
            blockedNumbers.collect { _blockedCount.value = it.size }
        }
        viewModelScope.launch {
            allowlistedNumbers.collect { _allowlistedCount.value = it.size }
        }
        viewModelScope.launch {
            patternRules.collect { _patternCount.value = it.size }
        }
        viewModelScope.launch {
            countryRules.collect { _countryCount.value = it.size }
        }
        viewModelScope.launch {
            actionRules.collect { _actionCount.value = it.size }
        }
        viewModelScope.launch {
            scheduleRules.collect { _scheduleCount.value = it.size }
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

    fun addPatternRule(
        pattern: String,
        label: String?,
    ) {
        viewModelScope.launch {
            ruleRepository.addPatternRule(pattern, label)
        }
    }

    fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.togglePatternRule(id, enabled)
        }
    }

    fun removePatternRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removePatternRule(id)
        }
    }

    fun addCountryRule(
        countryCode: String,
        countryName: String,
    ) {
        viewModelScope.launch {
            ruleRepository.addCountryRule(countryCode, countryName)
        }
    }

    fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.toggleCountryRule(id, enabled)
        }
    }

    fun removeCountryRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeCountryRule(id)
        }
    }

    fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
        patternId: Long? = null,
    ) {
        viewModelScope.launch {
            ruleRepository.addActionRule(label, attempts, windowMinutes, patternId)
        }
    }

    fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.toggleActionRule(id, enabled)
        }
    }

    fun removeActionRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeActionRule(id)
        }
    }

    fun addScheduleRule(
        label: String?,
        startMinute: Int,
        endMinute: Int,
    ) {
        viewModelScope.launch {
            ruleRepository.addScheduleRule(label, startMinute, endMinute)
        }
    }

    fun toggleScheduleRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.toggleScheduleRule(id, enabled)
        }
    }

    fun removeScheduleRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeScheduleRule(id)
        }
    }
}

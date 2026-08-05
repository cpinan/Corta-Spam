package org.carlospinan.bloqueador.app.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RulePrecedenceResolver
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry

data class BlockListUiState(
    val blockedNumbers: List<BlockedNumberEntry> = emptyList(),
    val allowlistedNumbers: List<AllowlistedNumberEntry> = emptyList(),
    val patternRules: List<PatternRuleEntry> = emptyList(),
    val countryRules: List<CountryRuleEntry> = emptyList(),
    val scheduleRules: List<ScheduleRuleEntry> = emptyList(),
    val actionRules: List<ActionRuleEntry> = emptyList(),
    val contactNames: Map<String, String> = emptyMap(),
) {
    val blockedCount: Int get() = blockedNumbers.size
    val allowlistedCount: Int get() = allowlistedNumbers.size
    val patternCount: Int get() = patternRules.size
    val countryCount: Int get() = countryRules.size
    val scheduleCount: Int get() = scheduleRules.size
    val actionCount: Int get() = actionRules.size
}

sealed interface BlockListIntent {
    data class AddBlockedNumber(
        val number: String,
        val label: String? = null,
    ) : BlockListIntent

    data class RemoveBlockedNumber(
        val id: Long,
    ) : BlockListIntent

    data class AddAllowlistedNumber(
        val number: String,
        val label: String? = null,
    ) : BlockListIntent

    data class RemoveAllowlistedNumber(
        val id: Long,
    ) : BlockListIntent

    data class AddPatternRule(
        val pattern: String,
        val label: String?,
    ) : BlockListIntent

    data class TogglePatternRule(
        val id: Long,
        val enabled: Boolean,
    ) : BlockListIntent

    data class RemovePatternRule(
        val id: Long,
    ) : BlockListIntent

    data class AddCountryRule(
        val countryCode: String,
        val countryName: String,
    ) : BlockListIntent

    data class ToggleCountryRule(
        val id: Long,
        val enabled: Boolean,
    ) : BlockListIntent

    data class RemoveCountryRule(
        val id: Long,
    ) : BlockListIntent

    data class AddScheduleRule(
        val label: String?,
        val startMinute: Int,
        val endMinute: Int,
    ) : BlockListIntent

    data class ToggleScheduleRule(
        val id: Long,
        val enabled: Boolean,
    ) : BlockListIntent

    data class RemoveScheduleRule(
        val id: Long,
    ) : BlockListIntent

    /**
     * @param patternId optional scope: only count attempts from numbers matching that pattern.
     *   Scoping to a pattern that is still *enabled* as a block rule has no effect — those
     *   numbers are blocked outright before any attempt counting happens.
     */
    data class AddActionRule(
        val label: String?,
        val attempts: Int,
        val windowMinutes: Int,
        val patternId: Long? = null,
    ) : BlockListIntent

    data class ToggleActionRule(
        val id: Long,
        val enabled: Boolean,
    ) : BlockListIntent

    data class RemoveActionRule(
        val id: Long,
    ) : BlockListIntent
}

/** Backs all 6 block-list screens (hub + 5 detail screens) -- a single instance doesn't know
 * which screen it's rendering for, so it aggregates everything all of them could need. */
class BlockListViewModel(
    private val ruleRepository: RuleRepository,
    private val contactsGateway: ContactsGateway,
) : ViewModel() {
    private val contactNamesFlow = MutableStateFlow<Map<String, String>>(emptyMap())

    val state: StateFlow<BlockListUiState> =
        combine(
            ruleRepository.blockedNumbers(),
            ruleRepository.allowlistedNumbers(),
            ruleRepository.patternRules(),
            ruleRepository.countryRules(),
            ruleRepository.scheduleRules(),
        ) { blocked, allowlisted, patterns, countries, schedules ->
            PartialBlockListState(blocked, allowlisted, patterns, countries, schedules)
            // kotlinx.coroutines' typed combine() overloads top out at 5 flows; chaining a plain
            // 2-arg combine on top avoids forcing the mixed-type set into the vararg Array<T> form.
        }.combine(ruleRepository.actionRules()) { partial, actionRules ->
            partial.copy(actionRules = actionRules)
        }.combine(contactNamesFlow) { partial, contactNames ->
            BlockListUiState(
                blockedNumbers = partial.blockedNumbers,
                allowlistedNumbers = partial.allowlistedNumbers,
                patternRules = partial.patternRules,
                countryRules = partial.countryRules,
                scheduleRules = partial.scheduleRules,
                actionRules = partial.actionRules,
                contactNames = contactNames,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockListUiState())

    init {
        if (contactsGateway.hasPermission()) {
            viewModelScope.launch {
                contactNamesFlow.value = contactsGateway.contactNames()
            }
        }
    }

    fun onIntent(intent: BlockListIntent) {
        when (intent) {
            is BlockListIntent.AddBlockedNumber -> addBlockedNumber(intent.number, intent.label)
            is BlockListIntent.RemoveBlockedNumber -> removeBlockedNumber(intent.id)
            is BlockListIntent.AddAllowlistedNumber -> addAllowlistedNumber(intent.number, intent.label)
            is BlockListIntent.RemoveAllowlistedNumber -> removeAllowlistedNumber(intent.id)
            is BlockListIntent.AddPatternRule -> addPatternRule(intent.pattern, intent.label)
            is BlockListIntent.TogglePatternRule -> togglePatternRule(intent.id, intent.enabled)
            is BlockListIntent.RemovePatternRule -> removePatternRule(intent.id)
            is BlockListIntent.AddCountryRule -> addCountryRule(intent.countryCode, intent.countryName)
            is BlockListIntent.ToggleCountryRule -> toggleCountryRule(intent.id, intent.enabled)
            is BlockListIntent.RemoveCountryRule -> removeCountryRule(intent.id)
            is BlockListIntent.AddScheduleRule -> addScheduleRule(intent.label, intent.startMinute, intent.endMinute)
            is BlockListIntent.ToggleScheduleRule -> toggleScheduleRule(intent.id, intent.enabled)
            is BlockListIntent.RemoveScheduleRule -> removeScheduleRule(intent.id)
            is BlockListIntent.AddActionRule ->
                addActionRule(intent.label, intent.attempts, intent.windowMinutes, intent.patternId)
            is BlockListIntent.ToggleActionRule -> toggleActionRule(intent.id, intent.enabled)
            is BlockListIntent.RemoveActionRule -> removeActionRule(intent.id)
        }
    }

    private fun addBlockedNumber(
        number: String,
        label: String?,
    ) {
        if (number.isBlank()) return
        viewModelScope.launch {
            ruleRepository.addBlockedNumber(number, label)
        }
    }

    private fun removeBlockedNumber(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeBlockedNumber(id)
        }
    }

    private fun addAllowlistedNumber(
        number: String,
        label: String?,
    ) {
        if (number.isBlank()) return
        viewModelScope.launch {
            ruleRepository.addAllowlistedNumber(number, label)
        }
    }

    private fun removeAllowlistedNumber(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeAllowlistedNumber(id)
        }
    }

    private fun addPatternRule(
        pattern: String,
        label: String?,
    ) {
        // The dialog already disables its confirm button for these, but the rule set is the
        // thing that must never hold a pattern that matches every number -- one saved "*" blocks
        // the user's entire phone with no obvious cause.
        if (!RulePrecedenceResolver.isUsablePattern(pattern)) return
        viewModelScope.launch {
            ruleRepository.addPatternRule(pattern, label)
        }
    }

    private fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.togglePatternRule(id, enabled)
        }
    }

    private fun removePatternRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removePatternRule(id)
        }
    }

    private fun addCountryRule(
        countryCode: String,
        countryName: String,
    ) {
        viewModelScope.launch {
            ruleRepository.addCountryRule(countryCode, countryName)
        }
    }

    private fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.toggleCountryRule(id, enabled)
        }
    }

    private fun removeCountryRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeCountryRule(id)
        }
    }

    private fun addScheduleRule(
        label: String?,
        startMinute: Int,
        endMinute: Int,
    ) {
        viewModelScope.launch {
            ruleRepository.addScheduleRule(label, startMinute, endMinute)
        }
    }

    private fun toggleScheduleRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.toggleScheduleRule(id, enabled)
        }
    }

    private fun removeScheduleRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeScheduleRule(id)
        }
    }

    private fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
        patternId: Long?,
    ) {
        // attempts <= 0 satisfies "count >= attempts" for every caller and would block the whole
        // phone; a zero-length window can never contain an attempt, so the rule is inert. The
        // dialog rejects both, and so does backup restore -- see BackupEntryValidator.
        if (attempts < 1 || windowMinutes < 1) return
        viewModelScope.launch {
            ruleRepository.addActionRule(label, attempts, windowMinutes, patternId)
        }
    }

    private fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            ruleRepository.toggleActionRule(id, enabled)
        }
    }

    private fun removeActionRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.removeActionRule(id)
        }
    }
}

private data class PartialBlockListState(
    val blockedNumbers: List<BlockedNumberEntry>,
    val allowlistedNumbers: List<AllowlistedNumberEntry>,
    val patternRules: List<PatternRuleEntry>,
    val countryRules: List<CountryRuleEntry>,
    val scheduleRules: List<ScheduleRuleEntry>,
    val actionRules: List<ActionRuleEntry> = emptyList(),
)

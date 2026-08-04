package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.carlospinan.bloqueador.app.backup.ImportResult
import org.carlospinan.bloqueador.app.rules.ActionRule
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRule
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.ScheduleRule
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry

/**
 * In-memory [RuleRepository] for tests.
 *
 * Seed reads by assigning to the `*Flow` properties; the snapshot accessors
 * ([blockedNumberEntries], [enabledCountryRules], ...) read from the same flows,
 * so a single assignment drives both the reactive UI path and the resolver path.
 *
 * The `enabled*` snapshots that return domain types rather than entry types
 * ([enabledPatterns], [enabledActionRules], [enabledScheduleRules]) have their own
 * backing lists, since there is no entry-to-domain mapping to derive them from.
 *
 * Writes are recorded, not applied: [addBlockedNumber] appends to
 * [addBlockedNumberCalls] without touching [blockedNumbersFlow].
 */
internal class FakeRuleRepository : RuleRepository {
    val blockedNumbersFlow = MutableStateFlow(emptyList<BlockedNumberEntry>())
    val allowlistedNumbersFlow = MutableStateFlow(emptyList<AllowlistedNumberEntry>())
    val patternRulesFlow = MutableStateFlow(emptyList<PatternRuleEntry>())
    val countryRulesFlow = MutableStateFlow(emptyList<CountryRuleEntry>())
    val actionRulesFlow = MutableStateFlow(emptyList<ActionRuleEntry>())
    val scheduleRulesFlow = MutableStateFlow(emptyList<ScheduleRuleEntry>())

    var enabledPatternRules = emptyList<PatternRule>()
    var enabledActionRuleList = emptyList<ActionRule>()
    var enabledScheduleRuleList = emptyList<ScheduleRule>()

    /** Value returned by [countRecentAttempts], regardless of number or window. */
    var recentAttemptsForNumber = 0

    val recordedAttempts = mutableListOf<String>()
    val addBlockedNumberCalls = mutableListOf<Pair<String, String?>>()

    /** Payload [exportAll] returns. */
    var exportJson: String = "{}"

    /** Result [importAll] returns; the JSON it was handed is recorded in [importedJson]. */
    var importResult: ImportResult = ImportResult()
    val importedJson = mutableListOf<String>()

    /** When set, [exportAll] and [importAll] throw it instead — for exercising failure paths. */
    var backupFailure: Exception? = null

    override fun blockedNumbers() = blockedNumbersFlow

    override fun allowlistedNumbers() = allowlistedNumbersFlow

    override fun patternRules() = patternRulesFlow

    override fun countryRules() = countryRulesFlow

    override fun actionRules() = actionRulesFlow

    override fun scheduleRules() = scheduleRulesFlow

    override suspend fun blockedNumberEntries() = blockedNumbersFlow.value

    override suspend fun allowlistedNumberEntries() = allowlistedNumbersFlow.value

    override suspend fun enabledPatterns(): List<PatternRule> = enabledPatternRules

    override suspend fun enabledCountryRules(): List<CountryRuleEntry> = countryRulesFlow.value.filter { it.enabled }

    override suspend fun enabledActionRules(): List<ActionRule> = enabledActionRuleList

    override suspend fun enabledScheduleRules(): List<ScheduleRule> = enabledScheduleRuleList

    override suspend fun recordCallAttempt(
        number: String,
        timestampMillis: Long,
    ) {
        recordedAttempts += number
    }

    override suspend fun countRecentAttempts(
        number: String,
        sinceTimestampMillis: Long,
    ) = recentAttemptsForNumber

    override suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long) {}

    override suspend fun addBlockedNumber(
        number: String,
        label: String?,
    ) {
        addBlockedNumberCalls.add(number to label)
    }

    override suspend fun removeBlockedNumber(id: Long) {}

    override suspend fun addAllowlistedNumber(
        number: String,
        label: String?,
    ) {}

    override suspend fun removeAllowlistedNumber(id: Long) {}

    override suspend fun addPatternRule(
        pattern: String,
        label: String?,
    ) {}

    override suspend fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    ) {}

    override suspend fun removePatternRule(id: Long) {}

    override suspend fun addCountryRule(
        countryCode: String,
        countryName: String,
    ) {}

    override suspend fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    ) {}

    override suspend fun removeCountryRule(id: Long) {}

    override suspend fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
        patternId: Long?,
    ) {}

    override suspend fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    ) {}

    override suspend fun removeActionRule(id: Long) {}

    override suspend fun addScheduleRule(
        label: String?,
        startMinute: Int,
        endMinute: Int,
    ) {}

    override suspend fun toggleScheduleRule(
        id: Long,
        enabled: Boolean,
    ) {}

    override suspend fun removeScheduleRule(id: Long) {}

    override suspend fun exportAll(): String = backupFailure?.let { throw it } ?: exportJson

    override suspend fun importAll(json: String): ImportResult {
        backupFailure?.let { throw it }
        importedJson += json
        return importResult
    }
}

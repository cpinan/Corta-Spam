package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing blocking/allowlist rules.
 * All write operations emit updated Flow snapshots automatically.
 */
interface RuleRepository {
    /** All manually blocked numbers, most recent first. */
    fun blockedNumbers(): Flow<List<BlockedNumberEntry>>

    /** All allowlisted numbers, most recent first. */
    fun allowlistedNumbers(): Flow<List<AllowlistedNumberEntry>>

    /** All pattern rules, most recent first. */
    fun patternRules(): Flow<List<PatternRuleEntry>>

    /** All country rules, sorted by country name. */
    fun countryRules(): Flow<List<CountryRuleEntry>>

    /** All action rules, most recent first. */
    fun actionRules(): Flow<List<ActionRuleEntry>>

    /** Snapshot of current blocked numbers with full metadata (for resolver). */
    suspend fun blockedNumberEntries(): List<BlockedNumberEntry>

    /** Snapshot of current allowlisted numbers as a set (for resolver). */
    suspend fun allowlistedNumberSet(): Set<String>

    /** Snapshot of enabled pattern rules (for resolver). */
    suspend fun enabledPatterns(): List<PatternRule>

    /** Snapshot of enabled country rules with full metadata (for resolver). */
    suspend fun enabledCountryRules(): List<CountryRuleEntry>

    /** Snapshot of enabled action rules (for resolver). */
    suspend fun enabledActionRules(): List<ActionRule>

    /** Record an incoming call attempt for action-rule counting. */
    suspend fun recordCallAttempt(
        number: String,
        timestampMillis: Long,
    )

    /** Count attempts for [number] since [sinceTimestampMillis]. */
    suspend fun countRecentAttempts(
        number: String,
        sinceTimestampMillis: Long,
    ): Int

    /** Delete attempt rows older than [beforeTimestampMillis]. */
    suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long)

    suspend fun addBlockedNumber(
        number: String,
        label: String? = null,
    )

    suspend fun removeBlockedNumber(id: Long)

    suspend fun addAllowlistedNumber(
        number: String,
        label: String? = null,
    )

    suspend fun removeAllowlistedNumber(id: Long)

    suspend fun addPatternRule(
        pattern: String,
        label: String? = null,
    )

    suspend fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    )

    suspend fun removePatternRule(id: Long)

    suspend fun addCountryRule(
        countryCode: String,
        countryName: String,
    )

    suspend fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    )

    suspend fun removeCountryRule(id: Long)

    suspend fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
    )

    suspend fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    )

    suspend fun removeActionRule(id: Long)
}

data class BlockedNumberEntry(
    val id: Long,
    val number: String,
    val label: String?,
    val createdAt: Long,
)

data class AllowlistedNumberEntry(
    val id: Long,
    val number: String,
    val label: String?,
    val createdAt: Long,
)

data class PatternRuleEntry(
    val id: Long,
    val pattern: String,
    val label: String?,
    val enabled: Boolean,
    val createdAt: Long,
)

data class CountryRuleEntry(
    val id: Long,
    val countryCode: String,
    val countryName: String,
    val enabled: Boolean,
    val createdAt: Long,
)

data class ActionRuleEntry(
    val id: Long,
    val label: String?,
    val attempts: Int,
    val windowMinutes: Int,
    val enabled: Boolean,
    val createdAt: Long,
)

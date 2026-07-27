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

    /** Snapshot of current blocked numbers as a set (for resolver). */
    suspend fun blockedNumberSet(): Set<String>

    /** Snapshot of current allowlisted numbers as a set (for resolver). */
    suspend fun allowlistedNumberSet(): Set<String>

    /** Snapshot of enabled pattern rules (for resolver). */
    suspend fun enabledPatterns(): List<PatternRule>

    /** Snapshot of enabled country codes as a set (for resolver). */
    suspend fun enabledCountryCodeSet(): Set<String>

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

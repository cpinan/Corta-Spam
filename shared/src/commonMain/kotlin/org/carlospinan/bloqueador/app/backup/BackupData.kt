package org.carlospinan.bloqueador.app.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val blockedNumbers: List<BackupBlockedNumber> = emptyList(),
    val allowlistedNumbers: List<BackupAllowlistedNumber> = emptyList(),
    val patternRules: List<BackupPatternRule> = emptyList(),
    val countryRules: List<BackupCountryRule> = emptyList(),
    val actionRules: List<BackupActionRule> = emptyList(),
    val scheduleRules: List<BackupScheduleRule> = emptyList(),
)

@Serializable
data class BackupBlockedNumber(
    val number: String,
    val label: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupAllowlistedNumber(
    val number: String,
    val label: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupPatternRule(
    val pattern: String,
    val label: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long,
    /**
     * Row id in the database this backup came from. Only used to re-link
     * [BackupActionRule.patternId] on restore, where ids are reassigned by AUTOINCREMENT.
     * Nullable so backups written before this field existed still import (their action rules
     * just lose the pattern scope, which is what a dangling id meant anyway).
     */
    val id: Long? = null,
)

@Serializable
data class BackupCountryRule(
    val countryCode: String,
    val countryName: String,
    val enabled: Boolean = true,
    val createdAt: Long,
)

@Serializable
data class BackupActionRule(
    val label: String? = null,
    val attempts: Int,
    val windowMinutes: Int,
    val patternId: Long? = null,
    val enabled: Boolean = true,
    val createdAt: Long,
)

@Serializable
data class BackupScheduleRule(
    val label: String? = null,
    val startMinute: Int,
    val endMinute: Int,
    val enabled: Boolean = true,
    val createdAt: Long,
)

/**
 * Outcome of a restore. Every count is rows that actually landed in the database: numbers and
 * country codes are `INSERT OR IGNORE` against a UNIQUE column, so a backup that repeats an
 * entry the user already has adds nothing and must not be counted as if it did.
 *
 * [skipped] covers both of those already-present rows and entries rejected by
 * [org.carlospinan.bloqueador.app.rules.BackupEntryValidator] -- a hand-edited backup can carry
 * an action rule with `attempts = 0` (which would match every call) or a quiet-hours window
 * outside 0..1439, and neither is reachable through the app's own UI.
 */
data class ImportResult(
    val blockedNumbersImported: Int = 0,
    val allowlistedNumbersImported: Int = 0,
    val patternsImported: Int = 0,
    val countriesImported: Int = 0,
    val actionsImported: Int = 0,
    val schedulesImported: Int = 0,
    val skipped: Int = 0,
) {
    val total: Int
        get() =
            blockedNumbersImported + allowlistedNumbersImported +
                patternsImported + countriesImported +
                actionsImported + schedulesImported
}

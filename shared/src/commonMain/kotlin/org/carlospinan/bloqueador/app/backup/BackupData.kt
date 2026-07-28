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

data class ImportResult(
    val blockedNumbersImported: Int = 0,
    val allowlistedNumbersImported: Int = 0,
    val patternsImported: Int = 0,
    val countriesImported: Int = 0,
    val actionsImported: Int = 0,
    val schedulesImported: Int = 0,
) {
    val total: Int
        get() =
            blockedNumbersImported + allowlistedNumbersImported +
                patternsImported + countriesImported +
                actionsImported + schedulesImported
}

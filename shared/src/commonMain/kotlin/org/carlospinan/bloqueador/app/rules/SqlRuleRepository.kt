package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlRuleRepository(
    private val database: AppDatabase,
) : RuleRepository {
    private val queries get() = database.appDatabaseQueries

    override fun blockedNumbers(): Flow<List<BlockedNumberEntry>> =
        queries
            .selectAllBlockedNumbers()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    BlockedNumberEntry(
                        id = it.id,
                        number = it.number,
                        label = it.label,
                        createdAt = it.created_at,
                    )
                }
            }

    override fun allowlistedNumbers(): Flow<List<AllowlistedNumberEntry>> =
        queries
            .selectAllAllowlistedNumbers()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    AllowlistedNumberEntry(
                        id = it.id,
                        number = it.number,
                        label = it.label,
                        createdAt = it.created_at,
                    )
                }
            }

    override fun patternRules(): Flow<List<PatternRuleEntry>> =
        queries
            .selectAllPatternRules()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    PatternRuleEntry(
                        id = it.id,
                        pattern = it.pattern,
                        label = it.label,
                        enabled = it.enabled == 1L,
                        createdAt = it.created_at,
                    )
                }
            }

    override fun countryRules(): Flow<List<CountryRuleEntry>> =
        queries
            .selectAllCountryRules()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    CountryRuleEntry(
                        id = it.id,
                        countryCode = it.country_code,
                        countryName = it.country_name,
                        enabled = it.enabled == 1L,
                        createdAt = it.created_at,
                    )
                }
            }

    override fun actionRules(): Flow<List<ActionRuleEntry>> =
        queries
            .selectAllActionRules()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    ActionRuleEntry(
                        id = it.id,
                        label = it.label,
                        attempts = it.attempts.toInt(),
                        windowMinutes = it.window_minutes.toInt(),
                        enabled = it.enabled == 1L,
                        createdAt = it.created_at,
                    )
                }
            }

    override suspend fun blockedNumberSet(): Set<String> =
        withContext(Dispatchers.IO) {
            queries
                .selectAllBlockedNumbers()
                .executeAsList()
                .map { it.number }
                .toSet()
        }

    override suspend fun allowlistedNumberSet(): Set<String> =
        withContext(Dispatchers.IO) {
            queries
                .selectAllAllowlistedNumbers()
                .executeAsList()
                .map { it.number }
                .toSet()
        }

    override suspend fun enabledPatterns(): List<PatternRule> =
        withContext(Dispatchers.IO) {
            queries.selectAllPatternRules().executeAsList().filter { it.enabled == 1L }.map {
                PatternRule(
                    id = it.id,
                    pattern = it.pattern,
                    label = it.label,
                    enabled = true,
                )
            }
        }

    override suspend fun enabledCountryCodeSet(): Set<String> =
        withContext(Dispatchers.IO) {
            queries
                .selectAllCountryRules()
                .executeAsList()
                .filter { it.enabled == 1L }
                .map { it.country_code }
                .toSet()
        }

    override suspend fun enabledActionRules(): List<ActionRule> =
        withContext(Dispatchers.IO) {
            queries
                .selectAllActionRules()
                .executeAsList()
                .filter { it.enabled == 1L }
                .map {
                    ActionRule(
                        id = it.id,
                        label = it.label,
                        attempts = it.attempts.toInt(),
                        windowMinutes = it.window_minutes.toInt(),
                        enabled = true,
                    )
                }
        }

    override suspend fun recordCallAttempt(
        number: String,
        timestampMillis: Long,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertCallAttempt(number, timestampMillis).value
        }
    }

    override suspend fun countRecentAttempts(
        number: String,
        sinceTimestampMillis: Long,
    ): Int =
        withContext(Dispatchers.IO) {
            queries.countRecentAttempts(number, sinceTimestampMillis).executeAsOne().toInt()
        }

    override suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteExpiredAttempts(beforeTimestampMillis).value
        }
    }

    override suspend fun addBlockedNumber(
        number: String,
        label: String?,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertBlockedNumber(number, label).value
        }
    }

    override suspend fun removeBlockedNumber(id: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteBlockedNumberById(id).value
        }
    }

    override suspend fun addAllowlistedNumber(
        number: String,
        label: String?,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertAllowlistedNumber(number, label).value
        }
    }

    override suspend fun removeAllowlistedNumber(id: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteAllowlistedNumberById(id).value
        }
    }

    override suspend fun addPatternRule(
        pattern: String,
        label: String?,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertPatternRule(pattern, label).value
        }
    }

    override suspend fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            queries.togglePatternRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removePatternRule(id: Long) {
        withContext(Dispatchers.IO) {
            queries.deletePatternRuleById(id).value
        }
    }

    override suspend fun addCountryRule(
        countryCode: String,
        countryName: String,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertCountryRule(countryCode, countryName).value
        }
    }

    override suspend fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            queries.toggleCountryRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeCountryRule(id: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteCountryRuleById(id).value
        }
    }

    override suspend fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertActionRule(label, attempts.toLong(), windowMinutes.toLong()).value
        }
    }

    override suspend fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            queries.toggleActionRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeActionRule(id: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteActionRuleById(id).value
        }
    }
}

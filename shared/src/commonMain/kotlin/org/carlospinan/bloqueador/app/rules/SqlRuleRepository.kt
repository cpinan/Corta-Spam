package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.carlospinan.bloqueador.app.backup.BackupData
import org.carlospinan.bloqueador.app.backup.ImportResult
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlRuleRepository(
    private val database: AppDatabase,
) : RuleRepository {
    private val queries get() = database.appDatabaseQueries

    override fun blockedNumbers(): Flow<List<BlockedNumberEntry>> =
        queries
            .selectAllBlockedNumbers()
            .asFlow()
            .mapToList(Dispatchers.Default)
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
            .mapToList(Dispatchers.Default)
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
            .mapToList(Dispatchers.Default)
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
            .mapToList(Dispatchers.Default)
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
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    ActionRuleEntry(
                        id = it.id,
                        label = it.label,
                        attempts = it.attempts.toInt(),
                        windowMinutes = it.window_minutes.toInt(),
                        patternId = it.pattern_id,
                        enabled = it.enabled == 1L,
                        createdAt = it.created_at,
                    )
                }
            }

    override fun scheduleRules(): Flow<List<ScheduleRuleEntry>> =
        queries
            .selectAllScheduleRules()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    ScheduleRuleEntry(
                        id = it.id,
                        label = it.label,
                        startMinute = it.start_minute.toInt(),
                        endMinute = it.end_minute.toInt(),
                        enabled = it.enabled == 1L,
                        createdAt = it.created_at,
                    )
                }
            }

    override suspend fun blockedNumberEntries(): List<BlockedNumberEntry> =
        withContext(Dispatchers.Default) {
            queries.selectAllBlockedNumbers().executeAsList().map {
                BlockedNumberEntry(
                    id = it.id,
                    number = it.number,
                    label = it.label,
                    createdAt = it.created_at,
                )
            }
        }

    override suspend fun allowlistedNumberSet(): Set<String> =
        withContext(Dispatchers.Default) {
            queries
                .selectAllAllowlistedNumbers()
                .executeAsList()
                .map { it.number }
                .toSet()
        }

    override suspend fun enabledPatterns(): List<PatternRule> =
        withContext(Dispatchers.Default) {
            queries.selectAllPatternRules().executeAsList().filter { it.enabled == 1L }.map {
                PatternRule(
                    id = it.id,
                    pattern = it.pattern,
                    label = it.label,
                    enabled = true,
                )
            }
        }

    override suspend fun enabledCountryRules(): List<CountryRuleEntry> =
        withContext(Dispatchers.Default) {
            queries
                .selectAllCountryRules()
                .executeAsList()
                .filter { it.enabled == 1L }
                .map {
                    CountryRuleEntry(
                        id = it.id,
                        countryCode = it.country_code,
                        countryName = it.country_name,
                        enabled = true,
                        createdAt = it.created_at,
                    )
                }
        }

    override suspend fun enabledActionRules(): List<ActionRule> =
        withContext(Dispatchers.Default) {
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
                        patternId = it.pattern_id,
                        enabled = true,
                    )
                }
        }

    override suspend fun enabledScheduleRules(): List<ScheduleRule> =
        withContext(Dispatchers.Default) {
            queries
                .selectAllScheduleRules()
                .executeAsList()
                .filter { it.enabled == 1L }
                .map {
                    ScheduleRule(
                        id = it.id,
                        label = it.label,
                        startMinute = it.start_minute.toInt(),
                        endMinute = it.end_minute.toInt(),
                        enabled = true,
                    )
                }
        }

    override suspend fun recordCallAttempt(
        number: String,
        timestampMillis: Long,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertCallAttempt(number, timestampMillis).value
        }
    }

    override suspend fun countRecentAttempts(
        number: String,
        sinceTimestampMillis: Long,
    ): Int =
        withContext(Dispatchers.Default) {
            queries.countRecentAttempts(number, sinceTimestampMillis).executeAsOne().toInt()
        }

    override suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteExpiredAttempts(beforeTimestampMillis).value
        }
    }

    override suspend fun addBlockedNumber(
        number: String,
        label: String?,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertBlockedNumber(number, label).value
        }
    }

    override suspend fun removeBlockedNumber(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteBlockedNumberById(id).value
        }
    }

    override suspend fun addAllowlistedNumber(
        number: String,
        label: String?,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertAllowlistedNumber(number, label).value
        }
    }

    override suspend fun removeAllowlistedNumber(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteAllowlistedNumberById(id).value
        }
    }

    override suspend fun addPatternRule(
        pattern: String,
        label: String?,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertPatternRule(pattern, label).value
        }
    }

    override suspend fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.Default) {
            queries.togglePatternRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removePatternRule(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deletePatternRuleById(id).value
        }
    }

    override suspend fun addCountryRule(
        countryCode: String,
        countryName: String,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertCountryRule(countryCode, countryName).value
        }
    }

    override suspend fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.Default) {
            queries.toggleCountryRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeCountryRule(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteCountryRuleById(id).value
        }
    }

    override suspend fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
        patternId: Long?,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertActionRule(label, attempts.toLong(), windowMinutes.toLong(), patternId).value
        }
    }

    override suspend fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.Default) {
            queries.toggleActionRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeActionRule(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteActionRuleById(id).value
        }
    }

    override suspend fun addScheduleRule(
        label: String?,
        startMinute: Int,
        endMinute: Int,
    ) {
        withContext(Dispatchers.Default) {
            queries.insertScheduleRule(label, startMinute.toLong(), endMinute.toLong()).value
        }
    }

    override suspend fun toggleScheduleRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.Default) {
            queries.toggleScheduleRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeScheduleRule(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteScheduleRuleById(id).value
        }
    }

    override suspend fun exportAll(): String =
        withContext(Dispatchers.Default) {
            val now =
                kotlinx.datetime.Clock.System
                    .now()
                    .toEpochMilliseconds()
            val data =
                BackupData(
                    exportedAt = now,
                    blockedNumbers =
                        queries.selectAllBlockedNumbers().executeAsList().map {
                            org.carlospinan.bloqueador.app.backup.BackupBlockedNumber(
                                number = it.number,
                                label = it.label,
                                createdAt = it.created_at,
                            )
                        },
                    allowlistedNumbers =
                        queries.selectAllAllowlistedNumbers().executeAsList().map {
                            org.carlospinan.bloqueador.app.backup.BackupAllowlistedNumber(
                                number = it.number,
                                label = it.label,
                                createdAt = it.created_at,
                            )
                        },
                    patternRules =
                        queries.selectAllPatternRules().executeAsList().map {
                            org.carlospinan.bloqueador.app.backup.BackupPatternRule(
                                pattern = it.pattern,
                                label = it.label,
                                enabled = it.enabled == 1L,
                                createdAt = it.created_at,
                            )
                        },
                    countryRules =
                        queries.selectAllCountryRules().executeAsList().map {
                            org.carlospinan.bloqueador.app.backup.BackupCountryRule(
                                countryCode = it.country_code,
                                countryName = it.country_name,
                                enabled = it.enabled == 1L,
                                createdAt = it.created_at,
                            )
                        },
                    actionRules =
                        queries.selectAllActionRules().executeAsList().map {
                            org.carlospinan.bloqueador.app.backup.BackupActionRule(
                                label = it.label,
                                attempts = it.attempts.toInt(),
                                windowMinutes = it.window_minutes.toInt(),
                                patternId = it.pattern_id,
                                enabled = it.enabled == 1L,
                                createdAt = it.created_at,
                            )
                        },
                    scheduleRules =
                        queries.selectAllScheduleRules().executeAsList().map {
                            org.carlospinan.bloqueador.app.backup.BackupScheduleRule(
                                label = it.label,
                                startMinute = it.start_minute.toInt(),
                                endMinute = it.end_minute.toInt(),
                                enabled = it.enabled == 1L,
                                createdAt = it.created_at,
                            )
                        },
                )
            json.encodeToString(BackupData.serializer(), data)
        }

    override suspend fun importAll(jsonStr: String): ImportResult =
        withContext(Dispatchers.Default) {
            val data = json.decodeFromString(BackupData.serializer(), jsonStr)
            var blocked = 0
            var allowlisted = 0
            var patterns = 0
            var countries = 0
            var actions = 0
            var schedules = 0

            for (entry in data.blockedNumbers) {
                queries.insertBlockedNumber(entry.number, entry.label)
                blocked++
            }
            for (entry in data.allowlistedNumbers) {
                queries.insertAllowlistedNumber(entry.number, entry.label)
                allowlisted++
            }
            for (entry in data.patternRules) {
                queries.insertPatternRule(entry.pattern, entry.label)
                if (!entry.enabled) {
                    val inserted = queries.selectAllPatternRules().executeAsList().lastOrNull()
                    if (inserted != null) {
                        queries.togglePatternRule(0, inserted.id)
                    }
                }
                patterns++
            }
            for (entry in data.countryRules) {
                queries.insertCountryRule(entry.countryCode, entry.countryName)
                if (!entry.enabled) {
                    val inserted = queries.selectAllCountryRules().executeAsList().lastOrNull()
                    if (inserted != null) {
                        queries.toggleCountryRule(0, inserted.id)
                    }
                }
                countries++
            }
            for (entry in data.actionRules) {
                queries.insertActionRule(entry.label, entry.attempts.toLong(), entry.windowMinutes.toLong(), entry.patternId)
                if (!entry.enabled) {
                    val inserted = queries.selectAllActionRules().executeAsList().lastOrNull()
                    if (inserted != null) {
                        queries.toggleActionRule(0, inserted.id)
                    }
                }
                actions++
            }
            for (entry in data.scheduleRules) {
                queries.insertScheduleRule(entry.label, entry.startMinute.toLong(), entry.endMinute.toLong())
                if (!entry.enabled) {
                    val inserted = queries.selectAllScheduleRules().executeAsList().lastOrNull()
                    if (inserted != null) {
                        queries.toggleScheduleRule(0, inserted.id)
                    }
                }
                schedules++
            }

            ImportResult(
                blockedNumbersImported = blocked,
                allowlistedNumbersImported = allowlisted,
                patternsImported = patterns,
                countriesImported = countries,
                actionsImported = actions,
                schedulesImported = schedules,
            )
        }

    companion object {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
    }
}

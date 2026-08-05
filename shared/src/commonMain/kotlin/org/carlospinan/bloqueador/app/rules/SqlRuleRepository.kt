package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.carlospinan.bloqueador.app.backup.BackupData
import org.carlospinan.bloqueador.app.backup.ImportResult
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlRuleRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : RuleRepository {
    private val queries get() = database.appDatabaseQueries

    override fun blockedNumbers(): Flow<List<BlockedNumberEntry>> =
        queries
            .selectAllBlockedNumbers()
            .asFlow()
            .mapToList(dispatcher)
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
            .mapToList(dispatcher)
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
            .mapToList(dispatcher)
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
            .mapToList(dispatcher)
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
            .mapToList(dispatcher)
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
            .mapToList(dispatcher)
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
        withContext(dispatcher) {
            queries.selectAllBlockedNumbers().executeAsList().map {
                BlockedNumberEntry(
                    id = it.id,
                    number = it.number,
                    label = it.label,
                    createdAt = it.created_at,
                )
            }
        }

    override suspend fun allowlistedNumberEntries(): List<AllowlistedNumberEntry> =
        withContext(dispatcher) {
            queries.selectAllAllowlistedNumbers().executeAsList().map {
                AllowlistedNumberEntry(
                    id = it.id,
                    number = it.number,
                    label = it.label,
                    createdAt = it.created_at,
                )
            }
        }

    override suspend fun enabledPatterns(): List<PatternRule> =
        withContext(dispatcher) {
            queries.selectAllPatternRules().executeAsList().filter { it.enabled == 1L }.map {
                PatternRule(
                    id = it.id,
                    pattern = it.pattern,
                    label = it.label,
                    enabled = true,
                )
            }
        }

    override suspend fun allPatterns(): List<PatternRule> =
        withContext(dispatcher) {
            queries.selectAllPatternRules().executeAsList().map {
                PatternRule(
                    id = it.id,
                    pattern = it.pattern,
                    label = it.label,
                    enabled = it.enabled == 1L,
                )
            }
        }

    override suspend fun enabledCountryRules(): List<CountryRuleEntry> =
        withContext(dispatcher) {
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
        withContext(dispatcher) {
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
        withContext(dispatcher) {
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
        withContext(dispatcher) {
            queries.insertCallAttempt(number, timestampMillis).value
        }
    }

    override suspend fun countRecentAttempts(
        number: String,
        sinceTimestampMillis: Long,
    ): Int =
        withContext(dispatcher) {
            queries.countRecentAttempts(number, sinceTimestampMillis).executeAsOne().toInt()
        }

    override suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long) {
        withContext(dispatcher) {
            queries.deleteExpiredAttempts(beforeTimestampMillis).value
        }
    }

    override suspend fun addBlockedNumber(
        number: String,
        label: String?,
    ) {
        withContext(dispatcher) {
            queries.insertBlockedNumber(number, label).value
        }
    }

    override suspend fun removeBlockedNumber(id: Long) {
        withContext(dispatcher) {
            queries.deleteBlockedNumberById(id).value
        }
    }

    override suspend fun addAllowlistedNumber(
        number: String,
        label: String?,
    ) {
        withContext(dispatcher) {
            queries.insertAllowlistedNumber(number, label).value
        }
    }

    override suspend fun removeAllowlistedNumber(id: Long) {
        withContext(dispatcher) {
            queries.deleteAllowlistedNumberById(id).value
        }
    }

    override suspend fun addPatternRule(
        pattern: String,
        label: String?,
    ) {
        withContext(dispatcher) {
            queries.insertPatternRule(pattern, label).value
        }
    }

    override suspend fun togglePatternRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(dispatcher) {
            queries.togglePatternRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removePatternRule(id: Long) {
        withContext(dispatcher) {
            queries.deletePatternRuleById(id).value
        }
    }

    override suspend fun addCountryRule(
        countryCode: String,
        countryName: String,
    ) {
        withContext(dispatcher) {
            queries.insertCountryRule(countryCode, countryName).value
        }
    }

    override suspend fun toggleCountryRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(dispatcher) {
            queries.toggleCountryRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeCountryRule(id: Long) {
        withContext(dispatcher) {
            queries.deleteCountryRuleById(id).value
        }
    }

    override suspend fun addActionRule(
        label: String?,
        attempts: Int,
        windowMinutes: Int,
        patternId: Long?,
    ) {
        withContext(dispatcher) {
            queries.insertActionRule(label, attempts.toLong(), windowMinutes.toLong(), patternId).value
        }
    }

    override suspend fun toggleActionRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(dispatcher) {
            queries.toggleActionRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeActionRule(id: Long) {
        withContext(dispatcher) {
            queries.deleteActionRuleById(id).value
        }
    }

    override suspend fun addScheduleRule(
        label: String?,
        startMinute: Int,
        endMinute: Int,
    ) {
        withContext(dispatcher) {
            queries.insertScheduleRule(label, startMinute.toLong(), endMinute.toLong()).value
        }
    }

    override suspend fun toggleScheduleRule(
        id: Long,
        enabled: Boolean,
    ) {
        withContext(dispatcher) {
            queries.toggleScheduleRule(if (enabled) 1L else 0L, id).value
        }
    }

    override suspend fun removeScheduleRule(id: Long) {
        withContext(dispatcher) {
            queries.deleteScheduleRuleById(id).value
        }
    }

    override suspend fun exportAll(): String =
        withContext(dispatcher) {
            val now = currentTimeMillis()
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
                                // Carried only so importAll can re-link action rules scoped to
                                // this pattern; ids are reassigned on the importing device.
                                id = it.id,
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

    /**
     * Restore from a backup produced by [exportAll].
     *
     * Three things this deliberately does, each of which used to be wrong:
     *
     * 1. Rows are written with their real `enabled`/`created_at` in a single `*Full` insert. The
     *    previous two-step "insert a default row, then find it and toggle it off" read the last
     *    element of a `created_at DESC, id DESC` query -- the *oldest* row -- so importing any
     *    disabled rule silently disabled an unrelated one the user still wanted.
     * 2. Counts come from the insert's affected-row count, not the loop counter. Numbers and
     *    country codes are `INSERT OR IGNORE` against a UNIQUE column, so re-importing a backup
     *    the user already holds legitimately writes nothing and must report nothing.
     * 3. The whole thing runs in one transaction, so a malformed entry halfway down the file
     *    can't leave the rule set half-restored.
     */
    override suspend fun importAll(jsonStr: String): ImportResult =
        withContext(dispatcher) {
            val data = json.decodeFromString(BackupData.serializer(), jsonStr)
            val nowSeconds = currentTimeMillis() / 1000L
            var blocked = 0
            var allowlisted = 0
            var patterns = 0
            var countries = 0
            var actions = 0
            var schedules = 0
            var skipped = 0

            // Source pattern id -> id it was given here, so an action rule scoped to a pattern
            // still points at that same pattern after restore.
            val patternIdRemap = mutableMapOf<Long, Long>()

            queries.transaction {
                for (entry in data.blockedNumbers) {
                    if (!BackupEntryValidator.isValid(entry)) {
                        skipped++
                        continue
                    }
                    val inserted =
                        queries
                            .insertBlockedNumberFull(
                                number = entry.number,
                                label = entry.label,
                                created_at = BackupEntryValidator.createdAtOrNow(entry.createdAt, nowSeconds),
                            ).value
                    if (inserted > 0) blocked++ else skipped++
                }
                for (entry in data.allowlistedNumbers) {
                    if (!BackupEntryValidator.isValid(entry)) {
                        skipped++
                        continue
                    }
                    val inserted =
                        queries
                            .insertAllowlistedNumberFull(
                                number = entry.number,
                                label = entry.label,
                                created_at = BackupEntryValidator.createdAtOrNow(entry.createdAt, nowSeconds),
                            ).value
                    if (inserted > 0) allowlisted++ else skipped++
                }
                for (entry in data.patternRules) {
                    if (!BackupEntryValidator.isValid(entry)) {
                        skipped++
                        continue
                    }
                    queries.insertPatternRuleFull(
                        pattern = entry.pattern,
                        label = entry.label,
                        enabled = if (entry.enabled) 1L else 0L,
                        created_at = BackupEntryValidator.createdAtOrNow(entry.createdAt, nowSeconds),
                    )
                    entry.id?.let { sourceId ->
                        patternIdRemap[sourceId] = queries.lastInsertRowId().executeAsOne()
                    }
                    patterns++
                }
                for (entry in data.countryRules) {
                    if (!BackupEntryValidator.isValid(entry)) {
                        skipped++
                        continue
                    }
                    val inserted =
                        queries
                            .insertCountryRuleFull(
                                country_code = entry.countryCode,
                                country_name = entry.countryName,
                                enabled = if (entry.enabled) 1L else 0L,
                                created_at = BackupEntryValidator.createdAtOrNow(entry.createdAt, nowSeconds),
                            ).value
                    if (inserted > 0) countries++ else skipped++
                }
                for (entry in data.actionRules) {
                    if (!BackupEntryValidator.isValid(entry)) {
                        skipped++
                        continue
                    }
                    queries.insertActionRuleFull(
                        label = entry.label,
                        attempts = entry.attempts.toLong(),
                        window_minutes = entry.windowMinutes.toLong(),
                        // An id we can't re-link would dangle onto whatever row happens to hold
                        // it here, silently rescoping the rule. Dropping the scope is the safe
                        // reading of "we no longer know which pattern this meant".
                        pattern_id = entry.patternId?.let { patternIdRemap[it] },
                        enabled = if (entry.enabled) 1L else 0L,
                        created_at = BackupEntryValidator.createdAtOrNow(entry.createdAt, nowSeconds),
                    )
                    actions++
                }
                for (entry in data.scheduleRules) {
                    if (!BackupEntryValidator.isValid(entry)) {
                        skipped++
                        continue
                    }
                    queries.insertScheduleRuleFull(
                        label = entry.label,
                        start_minute = entry.startMinute.toLong(),
                        end_minute = entry.endMinute.toLong(),
                        enabled = if (entry.enabled) 1L else 0L,
                        created_at = BackupEntryValidator.createdAtOrNow(entry.createdAt, nowSeconds),
                    )
                    schedules++
                }
            }

            ImportResult(
                blockedNumbersImported = blocked,
                allowlistedNumbersImported = allowlisted,
                patternsImported = patterns,
                countriesImported = countries,
                actionsImported = actions,
                schedulesImported = schedules,
                skipped = skipped,
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

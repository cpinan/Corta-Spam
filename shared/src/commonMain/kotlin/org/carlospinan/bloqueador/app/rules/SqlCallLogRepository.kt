package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlCallLogRepository(
    private val database: AppDatabase,
) : CallLogRepository {
    private val queries get() = database.appDatabaseQueries

    override fun allEntries(): Flow<List<CallLogEntryData>> =
        queries
            .selectAllCallLogEntries()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toData() } }

    override fun recentEntries(limit: Int): Flow<List<CallLogEntryData>> =
        queries
            .selectRecentCallLogEntries(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toData() } }

    override suspend fun blockedCountToday(): Int =
        withContext(Dispatchers.IO) {
            queries.countBlockedCallsToday().executeAsOne().toInt()
        }

    override suspend fun blockedCountThisWeek(): Int =
        withContext(Dispatchers.IO) {
            queries.countBlockedCallsThisWeek().executeAsOne().toInt()
        }

    override suspend fun blockedCountThisMonth(): Int =
        withContext(Dispatchers.IO) {
            queries.countBlockedCallsThisMonth().executeAsOne().toInt()
        }

    override suspend fun blockedStats(): BlockedStats =
        withContext(Dispatchers.IO) {
            BlockedStats(
                today = queries.countBlockedCallsToday().executeAsOne().toInt(),
                thisWeek = queries.countBlockedCallsThisWeek().executeAsOne().toInt(),
                thisMonth = queries.countBlockedCallsThisMonth().executeAsOne().toInt(),
            )
        }

    override suspend fun logCall(
        number: String,
        timestamp: Long,
        decision: RuleDecision,
    ) {
        withContext(Dispatchers.IO) {
            queries
                .insertCallLogEntry(
                    number = number,
                    timestamp = timestamp,
                    action = if (decision.isBlocked) "BLOCKED" else "ALLOWED",
                    rule_type = decision.ruleTypeTag,
                    rule_id = null,
                    rule_detail = decision.blockReason,
                ).value
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            queries.clearCallLog().value
        }
    }

    private fun org.carlospinan.bloqueador.app.db.CallLogEntry.toData() =
        CallLogEntryData(
            id = id,
            number = number,
            timestamp = timestamp,
            action = action,
            ruleType = rule_type,
            ruleId = rule_id,
            ruleDetail = rule_detail,
        )
}

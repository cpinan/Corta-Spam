package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.flow.Flow

/**
 * Repository for call log entries. Write-heavy during active call screening;
 * read-heavy for the call log UI and stats.
 */
interface CallLogRepository {
    /** All call log entries, most recent first. */
    fun allEntries(): Flow<List<CallLogEntryData>>

    /** Recent entries for the dashboard (limited). */
    fun recentEntries(limit: Int = 10): Flow<List<CallLogEntryData>>

    /** Count of blocked calls in the current day. */
    suspend fun blockedCountToday(): Int

    /** Count of blocked calls in the current week. */
    suspend fun blockedCountThisWeek(): Int

    /** Count of blocked calls in the current month. */
    suspend fun blockedCountThisMonth(): Int

    /** Log a call screening decision. */
    suspend fun logCall(
        number: String,
        timestamp: Long,
        decision: RuleDecision,
    )

    /** Clear all call log entries. */
    suspend fun clearAll()
}

data class CallLogEntryData(
    val id: Long,
    val number: String,
    val timestamp: Long,
    val action: String,
    val ruleType: String?,
    val ruleId: Long?,
    val ruleDetail: String?,
)

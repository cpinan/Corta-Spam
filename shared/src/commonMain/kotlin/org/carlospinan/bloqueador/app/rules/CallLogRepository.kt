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

    /** Batched stats query — single DB round-trip instead of 3 sequential calls. */
    suspend fun blockedStats(): BlockedStats

    /** Count blocked calls per day for the last [daysBack] days. Returns ordered by date descending. */
    suspend fun blockedByDay(daysBack: Int): List<DayStat>

    /**
     * Emits once on collection and again every time the call log changes.
     *
     * A change signal rather than the data itself: the Home screen's counters are three
     * aggregate queries plus a pending-review count, and re-running those on demand is cheaper
     * than keeping four reactive queries alive. Without it Home only recomputed on creation and
     * on an explicit Refresh, so a call blocked while the app sat in the foreground didn't move
     * the "blocked today" number until the user navigated away and back.
     */
    fun changes(): Flow<Unit>

    /**
     * Log a call screening decision, returning the new row's id.
     *
     * The id matters because the row is written while the call is still live -- the decision
     * has to be logged even if everything after it fails -- so an auto-responder recording,
     * which only exists once the call has ended, has to find its row again afterwards via
     * [attachRecording].
     */
    suspend fun logCall(
        number: String,
        timestamp: Long,
        decision: RuleDecision,
    ): Long

    /** Point [entryId] at a finished recording. */
    suspend fun attachRecording(
        entryId: Long,
        path: String,
    )

    /** Delete [entryId]'s recording file and clear the column. Safe to call when there is none. */
    suspend fun deleteRecording(entryId: Long)

    /** Clear all call log entries, deleting every recording they reference. */
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
    /** Auto-responder recording for this call, or null -- which is the case for almost every row. */
    val recordingPath: String? = null,
)

data class BlockedStats(
    val today: Int,
    val thisWeek: Int,
    val thisMonth: Int,
    /** Total entries logged with no matching rule while defaultAction=ASK; see [RuleDecision.PendingReview]. */
    val pendingReview: Int = 0,
)

/**
 * Blocked-call count for one local calendar day.
 *
 * Carries [daysAgo] rather than a rendered label: the label used to be built here as
 * "Today"/"Yesterday"/"3d ago", which is English regardless of the four locales the app ships,
 * and the data layer has no access to string resources. Formatting happens at the presentation
 * edge, where `stringResource` is available.
 */
data class DayStat(
    /** 0 = today, 1 = yesterday, and so on. */
    val daysAgo: Int,
    val count: Int,
    val cutoffEpochMillis: Long,
)

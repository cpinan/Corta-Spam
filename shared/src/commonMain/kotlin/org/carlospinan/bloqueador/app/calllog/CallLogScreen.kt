package org.carlospinan.bloqueador.app.calllog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_cancel
import cortaspam.shared.generated.resources.call_log_action_allowlist
import cortaspam.shared.generated.resources.call_log_action_block
import cortaspam.shared.generated.resources.call_log_action_callback
import cortaspam.shared.generated.resources.call_log_action_copy
import cortaspam.shared.generated.resources.call_log_action_label
import cortaspam.shared.generated.resources.call_log_allowed_label
import cortaspam.shared.generated.resources.call_log_blocked_label
import cortaspam.shared.generated.resources.call_log_delete_recording
import cortaspam.shared.generated.resources.call_log_delete_recording_confirm
import cortaspam.shared.generated.resources.call_log_detail_placeholder
import cortaspam.shared.generated.resources.call_log_empty_hint
import cortaspam.shared.generated.resources.call_log_filter_all
import cortaspam.shared.generated.resources.call_log_filter_blocked
import cortaspam.shared.generated.resources.call_log_filter_incoming
import cortaspam.shared.generated.resources.call_log_filter_month
import cortaspam.shared.generated.resources.call_log_filter_outgoing
import cortaspam.shared.generated.resources.call_log_filter_today
import cortaspam.shared.generated.resources.call_log_filter_week
import cortaspam.shared.generated.resources.call_log_no_matches
import cortaspam.shared.generated.resources.call_log_outgoing_label
import cortaspam.shared.generated.resources.call_log_play_recording
import cortaspam.shared.generated.resources.call_log_recording_label
import cortaspam.shared.generated.resources.call_log_review_label
import cortaspam.shared.generated.resources.call_log_rule_label
import cortaspam.shared.generated.resources.call_log_search_hint
import cortaspam.shared.generated.resources.call_log_time_label
import cortaspam.shared.generated.resources.call_log_title
import cortaspam.shared.generated.resources.call_log_title_month
import cortaspam.shared.generated.resources.call_log_title_review
import cortaspam.shared.generated.resources.call_log_title_today
import cortaspam.shared.generated.resources.call_log_title_week
import cortaspam.shared.generated.resources.ic_allowlist
import cortaspam.shared.generated.resources.ic_blocked_number
import cortaspam.shared.generated.resources.ic_call_log
import cortaspam.shared.generated.resources.ic_unknown_call
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.contacts.contactDisplayName
import org.carlospinan.bloqueador.app.rules.CallDirection
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser.sameNumber
import org.carlospinan.bloqueador.app.rules.currentTimeMillis
import org.carlospinan.bloqueador.app.rules.storedBlockReasonText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CallLogScreen(
    entries: List<CallLogEntryData>,
    filter: String = "all",
    contactNames: Map<String, String> = emptyMap(),
    onBlockNumber: (String) -> Unit = {},
    onAllowlistNumber: (String) -> Unit = {},
    onCopyNumber: (String) -> Unit = {},
    onCallBack: (String) -> Unit = {},
    onBack: () -> Unit,
    onPlayRecording: (String) -> Unit = {},
    onDeleteRecording: (Long) -> Unit = {},
    callLogRequest: CallLogRequest? = null,
    /**
     * Changes the date window. Handled by the ViewModel rather than here because it re-queries,
     * unlike the direction filter and the search box, which only narrow what is already loaded.
     */
    onSelectTimeFilter: (String) -> Unit = {},
) {
    var selectedNumber by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<CallLogEntryData?>(null) }
    var directionFilter by rememberSaveable { mutableStateOf(CallLogDirectionFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    val windowSizeClass = rememberWindowSizeClass()

    val visibleEntries =
        remember(entries, directionFilter, query, contactNames) {
            filterCallLog(entries, directionFilter, query, contactNames)
        }

    // A notification tap arrives as a request to act on one caller, so the screen opens with that
    // caller's actions already up -- the point of the tap was Block/Call back/Copy, and making
    // the user find the row again in a log of hundreds is the navigation the notification was
    // supposed to save. Applied once per request id: dismissing the dialog must not reopen it on
    // the next recomposition.
    var appliedRequestId by remember { mutableStateOf(NO_REQUEST_APPLIED) }
    LaunchedEffect(callLogRequest, entries, windowSizeClass) {
        val request = callLogRequest ?: return@LaunchedEffect
        if (request.id == appliedRequestId) return@LaunchedEffect
        if (windowSizeClass == WindowSizeClass.Expanded) {
            // The two-pane layout has no dialog: its detail pane is driven by an entry, so the
            // request waits for the log to load rather than resolving to nothing.
            val entry =
                entries.firstOrNull { sameNumber(it.number, request.number) }
                    ?: return@LaunchedEffect
            selectedEntry = entry
        } else {
            selectedNumber = request.number
        }
        appliedRequestId = request.id
    }

    val title =
        when (filter) {
            "today" -> stringResource(Res.string.call_log_title_today)
            "week" -> stringResource(Res.string.call_log_title_week)
            "month" -> stringResource(Res.string.call_log_title_month)
            "review" -> stringResource(Res.string.call_log_title_review)
            else -> stringResource(Res.string.call_log_title)
        }

    val selected = selectedEntry

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (windowSizeClass == WindowSizeClass.Expanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CallLogListPane(
                        entries = visibleEntries,
                        allEntriesEmpty = entries.isEmpty(),
                        title = title,
                        contactNames = contactNames,
                        selectedEntry = selected,
                        onEntryTap = { selectedEntry = it },
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                        onPlayRecording = onPlayRecording,
                        onDeleteRecording = onDeleteRecording,
                        filterBar = {
                            CallLogFilterBar(
                                directionFilter = directionFilter,
                                onDirectionFilter = { directionFilter = it },
                                timeFilter = filter,
                                onTimeFilter = onSelectTimeFilter,
                                query = query,
                                onQuery = { query = it },
                            )
                        },
                    )
                    CallLogDetailPane(
                        entry = selected,
                        contactNames = contactNames,
                        onBlockNumber = { number ->
                            onBlockNumber(number)
                            selectedEntry = null
                        },
                        onAllowlistNumber = { number ->
                            onAllowlistNumber(number)
                            selectedEntry = null
                        },
                        onCopyNumber = { number ->
                            onCopyNumber(number)
                            selectedEntry = null
                        },
                        onCallBack = { number ->
                            onCallBack(number)
                            selectedEntry = null
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                AdaptiveContent(windowSizeClass = windowSizeClass) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CallLogFilterBar(
                        directionFilter = directionFilter,
                        onDirectionFilter = { directionFilter = it },
                        timeFilter = filter,
                        onTimeFilter = onSelectTimeFilter,
                        query = query,
                        onQuery = { query = it },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (entries.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.call_log_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (visibleEntries.isEmpty()) {
                        // An empty log and an empty *filter result* are different problems, and
                        // the hint for the first one ("add a number to start blocking") is
                        // nonsense advice for the second.
                        Text(
                            text = stringResource(Res.string.call_log_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn {
                            items(visibleEntries, key = { it.id }) { entry ->
                                CallLogEntryRow(
                                    entry = entry,
                                    contactNames = contactNames,
                                    onTap = { selectedNumber = entry.number },
                                    onPlayRecording = onPlayRecording,
                                    onDeleteRecording = onDeleteRecording,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (windowSizeClass != WindowSizeClass.Expanded) {
        selectedNumber?.let { number ->
            AlertDialog(
                onDismissRequest = { selectedNumber = null },
                title = { Text(contactDisplayName(number, contactNames)) },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                onBlockNumber(number)
                                selectedNumber = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.call_log_action_block))
                        }
                        TextButton(
                            onClick = {
                                onAllowlistNumber(number)
                                selectedNumber = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.call_log_action_allowlist))
                        }
                        TextButton(
                            onClick = {
                                onCallBack(number)
                                selectedNumber = null
                            },
                            enabled = number.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.call_log_action_callback))
                        }
                        TextButton(
                            onClick = {
                                onCopyNumber(number)
                                selectedNumber = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.call_log_action_copy))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedNumber = null }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
            )
        }
    }
}

/**
 * Search box, direction/outcome chips, and date-window chips.
 *
 * Two rows of chips rather than one: they answer different questions ("which calls" and "when"),
 * and a single row that mixed them would let the user pick two things that look mutually
 * exclusive and are not. Each row scrolls horizontally, because the labels are translated and a
 * row that fits in English wraps or clips in Spanish.
 */
@Composable
private fun CallLogFilterBar(
    directionFilter: CallLogDirectionFilter,
    onDirectionFilter: (CallLogDirectionFilter) -> Unit,
    timeFilter: String,
    onTimeFilter: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text(stringResource(Res.string.call_log_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DIRECTION_CHIPS.forEach { (value, label) ->
                FilterChip(
                    selected = directionFilter == value,
                    onClick = { onDirectionFilter(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TIME_CHIPS.forEach { (value, label) ->
                FilterChip(
                    // "review" is reachable from Home but has no chip: it is a queue to work
                    // through, not a date window, and it would sit in this row claiming to be one.
                    selected = timeFilter == value,
                    onClick = { onTimeFilter(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}

@Composable
private fun CallLogListPane(
    entries: List<CallLogEntryData>,
    allEntriesEmpty: Boolean,
    title: String,
    contactNames: Map<String, String>,
    selectedEntry: CallLogEntryData?,
    onEntryTap: (CallLogEntryData) -> Unit,
    modifier: Modifier = Modifier,
    onPlayRecording: (String) -> Unit = {},
    onDeleteRecording: (Long) -> Unit = {},
    filterBar: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        filterBar()

        Spacer(modifier = Modifier.height(12.dp))

        if (allEntriesEmpty) {
            Text(
                text = stringResource(Res.string.call_log_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (entries.isEmpty()) {
            Text(
                text = stringResource(Res.string.call_log_no_matches),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(entries, key = { it.id }) { entry ->
                    val isSelected = selectedEntry?.id == entry.id
                    CallLogEntryRow(
                        entry = entry,
                        contactNames = contactNames,
                        onTap = { onEntryTap(entry) },
                        modifier =
                            if (isSelected) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(12.dp),
                                )
                            } else {
                                Modifier
                            },
                        onPlayRecording = onPlayRecording,
                        onDeleteRecording = onDeleteRecording,
                    )
                }
            }
        }
    }
}

@Composable
private fun CallLogDetailPane(
    entry: CallLogEntryData?,
    contactNames: Map<String, String>,
    onBlockNumber: (String) -> Unit,
    onAllowlistNumber: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onCallBack: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(28.dp),
    ) {
        if (entry == null) {
            Text(
                text = stringResource(Res.string.call_log_detail_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val isBlocked = entry.action == "BLOCKED"
            val isReview = entry.ruleType == "REVIEW"
            val actionColor =
                when {
                    isBlocked -> MaterialTheme.colorScheme.error
                    isReview -> MaterialTheme.colorScheme.tertiary
                    else -> Color(0xFF4CAF50)
                }
            val statusIcon =
                when {
                    isBlocked -> Res.drawable.ic_blocked_number
                    isReview -> Res.drawable.ic_unknown_call
                    else -> Res.drawable.ic_allowlist
                }
            val statusLabel =
                when {
                    isBlocked -> stringResource(Res.string.call_log_blocked_label)
                    isReview -> stringResource(Res.string.call_log_review_label)
                    else -> stringResource(Res.string.call_log_allowed_label)
                }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(statusIcon),
                    contentDescription = statusLabel,
                    tint = actionColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = contactDisplayName(entry.number, contactNames),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(18.dp))

            CallLogDetailRow(stringResource(Res.string.call_log_action_label), statusLabel, actionColor)
            // rule_detail is stored as structured data, not prose, so the log re-renders in
            // whatever locale the reader is using now rather than the one it was written in.
            val reasonText = storedBlockReasonText(entry.ruleDetail)
            if (reasonText != null) {
                CallLogDetailRow(stringResource(Res.string.call_log_rule_label), reasonText)
            }
            CallLogDetailRow(
                stringResource(Res.string.call_log_time_label),
                formatTimestamp(entry.timestamp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onCallBack(entry.number) },
                    enabled = entry.number.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.call_log_action_callback))
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { onAllowlistNumber(entry.number) },
                ) {
                    Text(stringResource(Res.string.call_log_action_allowlist))
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { onBlockNumber(entry.number) },
                ) {
                    Text(stringResource(Res.string.call_log_action_block))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { onCopyNumber(entry.number) }) {
                Text(stringResource(Res.string.call_log_action_copy))
            }
        }
    }
}

@Composable
private fun CallLogDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun CallLogEntryRow(
    entry: CallLogEntryData,
    contactNames: Map<String, String>,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayRecording: (String) -> Unit = {},
    onDeleteRecording: (Long) -> Unit = {},
) {
    val isBlocked = entry.action == "BLOCKED"
    val isReview = entry.ruleType == "REVIEW"
    // Direction is read first: an outgoing call carries no rule decision at all, so labelling it
    // "Allowed call" would report a screening result for a call that was never screened.
    val isOutgoing = entry.direction == CallDirection.OUTGOING
    val actionColor =
        when {
            isOutgoing -> MaterialTheme.colorScheme.onSurfaceVariant
            isBlocked -> MaterialTheme.colorScheme.error
            isReview -> MaterialTheme.colorScheme.tertiary
            else -> Color(0xFF4CAF50)
        }
    val statusIcon =
        when {
            isOutgoing -> Res.drawable.ic_call_log
            isBlocked -> Res.drawable.ic_blocked_number
            isReview -> Res.drawable.ic_unknown_call
            else -> Res.drawable.ic_allowlist
        }
    val statusLabel =
        when {
            isOutgoing -> stringResource(Res.string.call_log_outgoing_label)
            isBlocked -> stringResource(Res.string.call_log_blocked_label)
            isReview -> stringResource(Res.string.call_log_review_label)
            else -> stringResource(Res.string.call_log_allowed_label)
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onTap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(statusIcon),
                contentDescription = statusLabel,
                tint = actionColor,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contactDisplayName(entry.number, contactNames),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = storedBlockReasonText(entry.ruleDetail) ?: statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = actionColor,
            )
        }

        // Rendered on the row rather than in the tap dialog because that dialog is keyed by
        // phone number, and a recording belongs to one specific call, not to every call from
        // that number. It also means a recording is visible without tapping anything.
        entry.recordingPath?.let { path ->
            RecordingControls(
                onPlay = { onPlayRecording(path) },
                onDelete = { onDeleteRecording(entry.id) },
            )
        }
    }
}

@Composable
private fun RecordingControls(
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.call_log_recording_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onPlay) {
            Text(stringResource(Res.string.call_log_play_recording))
        }
        TextButton(onClick = { confirmingDelete = true }) {
            Text(
                text = stringResource(Res.string.call_log_delete_recording),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    // Deleting audio of a person's voice is not undoable and there is no trash to recover it
    // from, so it asks first -- unlike the other row actions, which are all reversible.
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(Res.string.call_log_delete_recording)) },
            text = { Text(stringResource(Res.string.call_log_delete_recording_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(Res.string.call_log_delete_recording),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

private const val NO_REQUEST_APPLIED = -1L

private val DIRECTION_CHIPS =
    listOf(
        CallLogDirectionFilter.ALL to Res.string.call_log_filter_all,
        CallLogDirectionFilter.INCOMING to Res.string.call_log_filter_incoming,
        CallLogDirectionFilter.OUTGOING to Res.string.call_log_filter_outgoing,
        CallLogDirectionFilter.BLOCKED to Res.string.call_log_filter_blocked,
    )

/** The values [CallLogViewModel]'s date filter understands, minus "review", which is not a date. */
private val TIME_CHIPS =
    listOf(
        "all" to Res.string.call_log_filter_all,
        "today" to Res.string.call_log_filter_today,
        "week" to Res.string.call_log_filter_week,
        "month" to Res.string.call_log_filter_month,
    )

private fun formatTimestamp(epochMillis: Long): String {
    val now = currentTimeMillis()
    if (now - epochMillis < 60_000L) return "Now"
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month =
        local.month.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
            .take(3)
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "$month ${local.dayOfMonth}, ${local.year} · $hour:$minute"
}

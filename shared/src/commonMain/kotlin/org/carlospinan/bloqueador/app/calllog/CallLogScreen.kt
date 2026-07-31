package org.carlospinan.bloqueador.app.calllog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.action_cancel
import bloqueallamadas.shared.generated.resources.call_log_action_allowlist
import bloqueallamadas.shared.generated.resources.call_log_action_block
import bloqueallamadas.shared.generated.resources.call_log_action_callback
import bloqueallamadas.shared.generated.resources.call_log_action_copy
import bloqueallamadas.shared.generated.resources.call_log_action_label
import bloqueallamadas.shared.generated.resources.call_log_allowed_label
import bloqueallamadas.shared.generated.resources.call_log_blocked_label
import bloqueallamadas.shared.generated.resources.call_log_detail_placeholder
import bloqueallamadas.shared.generated.resources.call_log_empty_hint
import bloqueallamadas.shared.generated.resources.call_log_rule_label
import bloqueallamadas.shared.generated.resources.call_log_time_label
import bloqueallamadas.shared.generated.resources.call_log_title
import bloqueallamadas.shared.generated.resources.call_log_title_month
import bloqueallamadas.shared.generated.resources.call_log_title_today
import bloqueallamadas.shared.generated.resources.call_log_title_week
import bloqueallamadas.shared.generated.resources.ic_allowlist
import bloqueallamadas.shared.generated.resources.ic_blocked_number
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CallLogScreen(
    entries: List<CallLogEntryData>,
    filter: String = "all",
    onBlockNumber: (String) -> Unit = {},
    onAllowlistNumber: (String) -> Unit = {},
    onCopyNumber: (String) -> Unit = {},
    onCallBack: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    var selectedNumber by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<CallLogEntryData?>(null) }
    val windowSizeClass = rememberWindowSizeClass()

    val title =
        when (filter) {
            "today" -> stringResource(Res.string.call_log_title_today)
            "week" -> stringResource(Res.string.call_log_title_week)
            "month" -> stringResource(Res.string.call_log_title_month)
            else -> stringResource(Res.string.call_log_title)
        }

    val selected = selectedEntry

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (windowSizeClass == WindowSizeClass.Expanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CallLogListPane(
                        entries = entries,
                        title = title,
                        selectedEntry = selected,
                        onEntryTap = { selectedEntry = it },
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                    )
                    CallLogDetailPane(
                        entry = selected,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    if (entries.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.call_log_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn {
                            items(entries, key = { it.id }) { entry ->
                                CallLogEntryRow(
                                    entry = entry,
                                    onTap = { selectedNumber = entry.number },
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
                title = { Text(number) },
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

@Composable
private fun CallLogListPane(
    entries: List<CallLogEntryData>,
    title: String,
    selectedEntry: CallLogEntryData?,
    onEntryTap: (CallLogEntryData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Text(
                text = stringResource(Res.string.call_log_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(entries, key = { it.id }) { entry ->
                    val isSelected = selectedEntry?.id == entry.id
                    CallLogEntryRow(
                        entry = entry,
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
                    )
                }
            }
        }
    }
}

@Composable
private fun CallLogDetailPane(
    entry: CallLogEntryData?,
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
            val actionColor = if (isBlocked) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
            val statusIcon = if (isBlocked) Res.drawable.ic_blocked_number else Res.drawable.ic_allowlist

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(statusIcon),
                    contentDescription =
                        if (isBlocked) {
                            stringResource(
                                Res.string.call_log_blocked_label,
                            )
                        } else {
                            stringResource(Res.string.call_log_allowed_label)
                        },
                    tint = actionColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text =
                        if (isBlocked) {
                            stringResource(
                                Res.string.call_log_blocked_label,
                            )
                        } else {
                            stringResource(Res.string.call_log_allowed_label)
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.number,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(18.dp))

            CallLogDetailRow(stringResource(Res.string.call_log_action_label), entry.action, actionColor)
            if (entry.ruleDetail != null) {
                CallLogDetailRow(stringResource(Res.string.call_log_rule_label), entry.ruleDetail)
            }
            CallLogDetailRow(
                stringResource(Res.string.call_log_time_label),
                formatTimestamp(entry.timestamp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onCallBack(entry.number) },
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
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBlocked = entry.action == "BLOCKED"
    val actionColor = if (isBlocked) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
    val statusIcon =
        if (isBlocked) Res.drawable.ic_blocked_number else Res.drawable.ic_allowlist

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
                contentDescription =
                    if (isBlocked) {
                        stringResource(
                            Res.string.call_log_blocked_label,
                        )
                    } else {
                        stringResource(Res.string.call_log_allowed_label)
                    },
                tint = actionColor,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.number,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = entry.ruleDetail ?: entry.action,
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
                text = entry.action,
                style = MaterialTheme.typography.labelMedium,
                color = actionColor,
            )
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
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

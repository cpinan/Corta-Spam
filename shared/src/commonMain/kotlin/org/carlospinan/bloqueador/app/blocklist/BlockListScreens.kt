package org.carlospinan.bloqueador.app.blocklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.action_add
import bloqueallamadas.shared.generated.resources.action_cancel
import bloqueallamadas.shared.generated.resources.action_ok
import bloqueallamadas.shared.generated.resources.action_remove
import bloqueallamadas.shared.generated.resources.action_skip
import bloqueallamadas.shared.generated.resources.allowlist_add_hint
import bloqueallamadas.shared.generated.resources.allowlist_add_title
import bloqueallamadas.shared.generated.resources.allowlist_empty_hint
import bloqueallamadas.shared.generated.resources.allowlist_label_hint
import bloqueallamadas.shared.generated.resources.allowlist_title
import bloqueallamadas.shared.generated.resources.block_list_add_hint
import bloqueallamadas.shared.generated.resources.block_list_add_title
import bloqueallamadas.shared.generated.resources.block_list_empty_hint
import bloqueallamadas.shared.generated.resources.block_list_hub_title
import bloqueallamadas.shared.generated.resources.block_list_label_hint
import bloqueallamadas.shared.generated.resources.block_list_manual_title
import bloqueallamadas.shared.generated.resources.block_list_title
import bloqueallamadas.shared.generated.resources.blocklist_add_allowlist
import bloqueallamadas.shared.generated.resources.blocklist_add_block
import bloqueallamadas.shared.generated.resources.blocklist_duplicate_allowlist_body
import bloqueallamadas.shared.generated.resources.blocklist_duplicate_allowlist_title
import bloqueallamadas.shared.generated.resources.blocklist_duplicate_blocked_body
import bloqueallamadas.shared.generated.resources.blocklist_duplicate_blocked_title
import bloqueallamadas.shared.generated.resources.country_add_title
import bloqueallamadas.shared.generated.resources.country_empty_hint
import bloqueallamadas.shared.generated.resources.country_search_hint
import bloqueallamadas.shared.generated.resources.country_title
import bloqueallamadas.shared.generated.resources.hub_countries
import bloqueallamadas.shared.generated.resources.hub_patterns
import bloqueallamadas.shared.generated.resources.hub_schedules
import bloqueallamadas.shared.generated.resources.ic_allowlist
import bloqueallamadas.shared.generated.resources.ic_blocked_number
import bloqueallamadas.shared.generated.resources.ic_countries
import bloqueallamadas.shared.generated.resources.ic_delete
import bloqueallamadas.shared.generated.resources.ic_patterns
import bloqueallamadas.shared.generated.resources.ic_quiet_hours
import bloqueallamadas.shared.generated.resources.pattern_add_hint
import bloqueallamadas.shared.generated.resources.pattern_add_title
import bloqueallamadas.shared.generated.resources.pattern_android_only
import bloqueallamadas.shared.generated.resources.pattern_empty_hint
import bloqueallamadas.shared.generated.resources.pattern_examples
import bloqueallamadas.shared.generated.resources.pattern_label_hint
import bloqueallamadas.shared.generated.resources.pattern_title
import bloqueallamadas.shared.generated.resources.schedule_empty_hint
import bloqueallamadas.shared.generated.resources.schedule_end_hint
import bloqueallamadas.shared.generated.resources.schedule_label_hint
import bloqueallamadas.shared.generated.resources.schedule_preset_night
import bloqueallamadas.shared.generated.resources.schedule_preset_siesta
import bloqueallamadas.shared.generated.resources.schedule_preset_work
import bloqueallamadas.shared.generated.resources.schedule_quick_presets
import bloqueallamadas.shared.generated.resources.schedule_rule_add_title
import bloqueallamadas.shared.generated.resources.schedule_rule_title
import bloqueallamadas.shared.generated.resources.schedule_select_time
import bloqueallamadas.shared.generated.resources.schedule_start_hint
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.rules.COUNTRIES
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManualBlockListScreen(
    numbers: List<org.carlospinan.bloqueador.app.rules.BlockedNumberEntry>,
    allowlistedNumbers: Set<String> = emptySet(),
    contactNames: Map<String, String> = emptyMap(),
    onAdd: (String, String?) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.block_list_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(Res.string.action_add))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (numbers.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.block_list_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(numbers, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName(entry.number, contactNames),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        if (entry.label != null) {
                                            Text(
                                                text = entry.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onRemove(entry.id) }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_delete),
                                            contentDescription = stringResource(Res.string.action_remove),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNumberDialog(
            title = stringResource(Res.string.block_list_add_title),
            hint = stringResource(Res.string.block_list_add_hint),
            labelHint = stringResource(Res.string.block_list_label_hint),
            onConfirm = { number, label ->
                if (number in allowlistedNumbers) {
                    pendingNumber = number
                    pendingLabel = label
                    showAddDialog = false
                    showDuplicateWarning = true
                } else {
                    onAdd(number, label)
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showDuplicateWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarning = false },
            title = { Text(stringResource(Res.string.blocklist_duplicate_allowlist_title)) },
            text = {
                Text(stringResource(Res.string.blocklist_duplicate_allowlist_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAdd(pendingNumber, pendingLabel)
                        showDuplicateWarning = false
                    },
                ) {
                    Text(stringResource(Res.string.blocklist_add_block))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateWarning = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
fun AllowlistScreen(
    numbers: List<org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry>,
    blockedNumbers: Set<String> = emptySet(),
    contactNames: Map<String, String> = emptyMap(),
    onAdd: (String, String?) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.allowlist_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(Res.string.action_add))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (numbers.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.allowlist_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(numbers, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName(entry.number, contactNames),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        if (entry.label != null) {
                                            Text(
                                                text = entry.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onRemove(entry.id) }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_delete),
                                            contentDescription = stringResource(Res.string.action_remove),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNumberDialog(
            title = stringResource(Res.string.allowlist_add_title),
            hint = stringResource(Res.string.allowlist_add_hint),
            labelHint = stringResource(Res.string.allowlist_label_hint),
            onConfirm = { number, label ->
                if (number in blockedNumbers) {
                    pendingNumber = number
                    pendingLabel = label
                    showAddDialog = false
                    showDuplicateWarning = true
                } else {
                    onAdd(number, label)
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showDuplicateWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarning = false },
            title = { Text(stringResource(Res.string.blocklist_duplicate_blocked_title)) },
            text = {
                Text(stringResource(Res.string.blocklist_duplicate_blocked_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAdd(pendingNumber, pendingLabel)
                        showDuplicateWarning = false
                    },
                ) {
                    Text(stringResource(Res.string.blocklist_add_allowlist))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateWarning = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
fun PatternRuleScreen(
    patterns: List<PatternRuleEntry>,
    onAdd: (String, String?) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.pattern_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.pattern_android_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(Res.string.action_add))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (patterns.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.pattern_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(patterns, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.pattern,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        if (entry.label != null) {
                                            Text(
                                                text = entry.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Row {
                                        androidx.compose.material3.Switch(
                                            checked = entry.enabled,
                                            onCheckedChange = { onToggle(entry.id, it) },
                                        )
                                        IconButton(onClick = { onRemove(entry.id) }) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_delete),
                                                contentDescription = stringResource(Res.string.action_remove),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPatternDialog(
            onConfirm = { pattern, label ->
                onAdd(pattern, label)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
fun CountryRuleScreen(
    countries: List<CountryRuleEntry>,
    onAdd: (String, String) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.country_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(Res.string.action_add))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (countries.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.country_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(countries, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.countryName} (+${entry.countryCode})",
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                    Row {
                                        androidx.compose.material3.Switch(
                                            checked = entry.enabled,
                                            onCheckedChange = { onToggle(entry.id, it) },
                                        )
                                        IconButton(onClick = { onRemove(entry.id) }) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_delete),
                                                contentDescription = stringResource(Res.string.action_remove),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCountryDialog(
            onConfirm = { code, name ->
                onAdd(code, name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockListHubScreen(
    blockedCount: Int,
    allowlistedCount: Int,
    patternCount: Int,
    countryCount: Int,
    scheduleCount: Int,
    onNavigateToManual: () -> Unit,
    onNavigateToAllowlist: () -> Unit,
    onNavigateToPatterns: () -> Unit,
    onNavigateToCountries: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.block_list_hub_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(24.dp))

                val columns =
                    when (windowSizeClass) {
                        WindowSizeClass.Compact -> 2
                        WindowSizeClass.Medium -> 4
                        WindowSizeClass.Expanded -> 4
                    }

                FlowRow(
                    maxItemsInEachRow = columns,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HubCard(
                        title = stringResource(Res.string.block_list_manual_title),
                        count = blockedCount,
                        icon = Res.drawable.ic_blocked_number,
                        onClick = onNavigateToManual,
                    )
                    HubCard(
                        title = stringResource(Res.string.allowlist_title),
                        count = allowlistedCount,
                        icon = Res.drawable.ic_allowlist,
                        onClick = onNavigateToAllowlist,
                    )
                    HubCard(
                        title = stringResource(Res.string.hub_patterns),
                        count = patternCount,
                        icon = Res.drawable.ic_patterns,
                        onClick = onNavigateToPatterns,
                    )
                    HubCard(
                        title = stringResource(Res.string.hub_countries),
                        count = countryCount,
                        icon = Res.drawable.ic_countries,
                        onClick = onNavigateToCountries,
                    )
                    HubCard(
                        title = stringResource(Res.string.hub_schedules),
                        count = scheduleCount,
                        icon = Res.drawable.ic_quiet_hours,
                        onClick = onNavigateToSchedules,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubCard(
    title: String,
    count: Int,
    icon: DrawableResource,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HubRow(
    title: String,
    count: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AddNumberDialog(
    title: String,
    hint: String,
    labelHint: String,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(hint) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(labelHint) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (number.isNotBlank()) onConfirm(number.trim(), label.trim().ifBlank { null })
                },
                enabled = number.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun AddPatternDialog(
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var pattern by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pattern_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(stringResource(Res.string.pattern_add_hint)) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(Res.string.pattern_label_hint)) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.pattern_examples),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pattern.isNotBlank()) {
                        onConfirm(pattern.trim(), label.trim().ifBlank { null })
                    }
                },
                enabled = pattern.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun AddCountryDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query) {
            if (query.isBlank()) {
                COUNTRIES
            } else {
                COUNTRIES.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.code.contains(query)
                }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.country_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.country_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filtered, key = { it.code + it.name }) { country ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onConfirm(country.code, country.name)
                                    }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = country.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "(+${country.code})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_skip))
            }
        },
    )
}

@Composable
fun ScheduleRuleScreen(
    rules: List<ScheduleRuleEntry>,
    onAdd: (String?, Int, Int) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.schedule_rule_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(Res.string.action_add))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (rules.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.schedule_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(rules, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text =
                                                "${minuteOfDayToHHmm(entry.startMinute)} – " +
                                                    minuteOfDayToHHmm(entry.endMinute),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        if (entry.label != null) {
                                            Text(
                                                text = entry.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    androidx.compose.material3.Switch(
                                        checked = entry.enabled,
                                        onCheckedChange = { onToggle(entry.id, it) },
                                    )
                                    IconButton(onClick = { onRemove(entry.id) }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_delete),
                                            contentDescription = stringResource(Res.string.action_remove),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleRuleDialog(
            onConfirm = { label, startMinute, endMinute ->
                onAdd(label, startMinute, endMinute)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

private fun displayName(
    number: String,
    contactNames: Map<String, String>,
): String = contactNames[PhoneNumberParser.normalizeForComparison(number)] ?: number

private fun minuteOfDayToHHmm(minute: Int): String {
    val hour = (minute / 60).toString().padStart(2, '0')
    val min = (minute % 60).toString().padStart(2, '0')
    return "$hour:$min"
}

/** Parses "HH:mm" into minutes since midnight (0-1439), or null if malformed/out of range. */
private fun parseHHmmToMinuteOfDay(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleRuleDialog(
    onConfirm: (String?, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(22) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(7) }
    var endMinute by remember { mutableStateOf(0) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startFormatted = "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}"
    val endFormatted = "${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.schedule_rule_add_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.schedule_start_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.TextButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(startFormatted, style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.schedule_end_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.TextButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(endFormatted, style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.schedule_quick_presets),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = false,
                        onClick = {
                            startHour = 22
                            startMinute = 0
                            endHour = 7
                            endMinute = 0
                        },
                        label = { Text(stringResource(Res.string.schedule_preset_night)) },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = false,
                        onClick = {
                            startHour = 14
                            startMinute = 0
                            endHour = 16
                            endMinute = 0
                        },
                        label = { Text(stringResource(Res.string.schedule_preset_siesta)) },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = false,
                        onClick = {
                            startHour = 9
                            startMinute = 0
                            endHour = 18
                            endMinute = 0
                        },
                        label = { Text(stringResource(Res.string.schedule_preset_work)) },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(Res.string.schedule_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = startHour * 60 + startMinute
                    val end = endHour * 60 + endMinute
                    onConfirm(label.ifBlank { null }, start, end)
                },
            ) {
                Text(stringResource(Res.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )

    if (showStartPicker) {
        TimePickerDialog(
            initialHour = startHour,
            initialMinute = startMinute,
            onConfirm = { h, m ->
                startHour = h
                startMinute = m
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            onConfirm = { h, m ->
                endHour = h
                endMinute = m
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.schedule_select_time)) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(Res.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

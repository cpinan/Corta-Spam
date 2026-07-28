package org.carlospinan.bloqueador.app.blocklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.action_add
import bloqueallamadas.shared.generated.resources.action_android_only
import bloqueallamadas.shared.generated.resources.action_attempts_hint
import bloqueallamadas.shared.generated.resources.action_cancel
import bloqueallamadas.shared.generated.resources.action_empty_hint
import bloqueallamadas.shared.generated.resources.action_label_hint
import bloqueallamadas.shared.generated.resources.action_rule_add_title
import bloqueallamadas.shared.generated.resources.action_rule_title
import bloqueallamadas.shared.generated.resources.action_window_hint
import bloqueallamadas.shared.generated.resources.allowlist_add_hint
import bloqueallamadas.shared.generated.resources.allowlist_add_title
import bloqueallamadas.shared.generated.resources.allowlist_empty_hint
import bloqueallamadas.shared.generated.resources.allowlist_title
import bloqueallamadas.shared.generated.resources.block_list_add_hint
import bloqueallamadas.shared.generated.resources.block_list_add_title
import bloqueallamadas.shared.generated.resources.block_list_empty_hint
import bloqueallamadas.shared.generated.resources.block_list_hub_title
import bloqueallamadas.shared.generated.resources.block_list_manual_title
import bloqueallamadas.shared.generated.resources.block_list_title
import bloqueallamadas.shared.generated.resources.country_add_title
import bloqueallamadas.shared.generated.resources.country_empty_hint
import bloqueallamadas.shared.generated.resources.country_search_hint
import bloqueallamadas.shared.generated.resources.country_title
import bloqueallamadas.shared.generated.resources.hub_actions
import bloqueallamadas.shared.generated.resources.hub_countries
import bloqueallamadas.shared.generated.resources.hub_patterns
import bloqueallamadas.shared.generated.resources.hub_schedules
import bloqueallamadas.shared.generated.resources.pattern_add_hint
import bloqueallamadas.shared.generated.resources.pattern_add_title
import bloqueallamadas.shared.generated.resources.pattern_android_only
import bloqueallamadas.shared.generated.resources.pattern_empty_hint
import bloqueallamadas.shared.generated.resources.pattern_label_hint
import bloqueallamadas.shared.generated.resources.pattern_title
import bloqueallamadas.shared.generated.resources.schedule_empty_hint
import bloqueallamadas.shared.generated.resources.schedule_end_hint
import bloqueallamadas.shared.generated.resources.schedule_invalid_time
import bloqueallamadas.shared.generated.resources.schedule_label_hint
import bloqueallamadas.shared.generated.resources.schedule_rule_add_title
import bloqueallamadas.shared.generated.resources.schedule_rule_title
import bloqueallamadas.shared.generated.resources.schedule_start_hint
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
import org.carlospinan.bloqueador.app.rules.COUNTRIES
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManualBlockListScreen(
    numbers: List<org.carlospinan.bloqueador.app.rules.BlockedNumberEntry>,
    onAdd: (String) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
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
                                            text = entry.number,
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
                                    TextButton(onClick = { onRemove(entry.id) }) {
                                        Text(
                                            text = "✕",
                                            color = MaterialTheme.colorScheme.error,
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
            onConfirm = { number ->
                onAdd(number)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
fun AllowlistScreen(
    numbers: List<org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry>,
    onAdd: (String) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
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
                                            text = entry.number,
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
                                    TextButton(onClick = { onRemove(entry.id) }) {
                                        Text(
                                            text = "✕",
                                            color = MaterialTheme.colorScheme.error,
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
            onConfirm = { number ->
                onAdd(number)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
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
                                        TextButton(onClick = { onRemove(entry.id) }) {
                                            Text(
                                                text = "✕",
                                                color = MaterialTheme.colorScheme.error,
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
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
                                        TextButton(onClick = { onRemove(entry.id) }) {
                                            Text(
                                                text = "✕",
                                                color = MaterialTheme.colorScheme.error,
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

@Composable
fun BlockListHubScreen(
    blockedCount: Int,
    allowlistedCount: Int,
    patternCount: Int,
    countryCount: Int,
    actionCount: Int,
    scheduleCount: Int,
    onNavigateToManual: () -> Unit,
    onNavigateToAllowlist: () -> Unit,
    onNavigateToPatterns: () -> Unit,
    onNavigateToCountries: () -> Unit,
    onNavigateToActions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onBack: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.block_list_hub_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(24.dp))

                HubRow(
                    title = stringResource(Res.string.block_list_manual_title),
                    count = blockedCount,
                    onClick = onNavigateToManual,
                )

                Spacer(modifier = Modifier.height(8.dp))

                HubRow(
                    title = stringResource(Res.string.allowlist_title),
                    count = allowlistedCount,
                    onClick = onNavigateToAllowlist,
                )

                Spacer(modifier = Modifier.height(8.dp))

                HubRow(
                    title = stringResource(Res.string.hub_patterns),
                    count = patternCount,
                    onClick = onNavigateToPatterns,
                )

                Spacer(modifier = Modifier.height(8.dp))

                HubRow(
                    title = stringResource(Res.string.hub_countries),
                    count = countryCount,
                    onClick = onNavigateToCountries,
                )

                Spacer(modifier = Modifier.height(8.dp))

                HubRow(
                    title = stringResource(Res.string.hub_actions),
                    count = actionCount,
                    onClick = onNavigateToActions,
                )

                Spacer(modifier = Modifier.height(8.dp))

                HubRow(
                    title = stringResource(Res.string.hub_schedules),
                    count = scheduleCount,
                    onClick = onNavigateToSchedules,
                )
            }
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
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var number by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text(hint) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (number.isNotBlank()) onConfirm(number.trim()) },
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
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
fun ActionRuleScreen(
    rules: List<ActionRuleEntry>,
    onAdd: (String?, Int, Int) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.action_rule_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.action_android_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(Res.string.action_add))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (rules.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.action_empty_hint),
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
                                            text = "${entry.attempts} calls / ${entry.windowMinutes} min",
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
                                    TextButton(onClick = { onRemove(entry.id) }) {
                                        Text(
                                            text = "✕",
                                            color = MaterialTheme.colorScheme.error,
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
        AddActionRuleDialog(
            onConfirm = { label, attempts, windowMinutes ->
                onAdd(label, attempts, windowMinutes)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AddActionRuleDialog(
    onConfirm: (String?, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var attemptsText by remember { mutableStateOf("3") }
    var windowText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.action_rule_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = attemptsText,
                    onValueChange = { attemptsText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text(stringResource(Res.string.action_attempts_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = windowText,
                    onValueChange = { windowText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text(stringResource(Res.string.action_window_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(Res.string.action_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val attempts = attemptsText.toIntOrNull() ?: return@TextButton
                    val window = windowText.toIntOrNull() ?: return@TextButton
                    if (attempts < 1 || window < 1) return@TextButton
                    onConfirm(label.ifBlank { null }, attempts, window)
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
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
                                    TextButton(onClick = { onRemove(entry.id) }) {
                                        Text(
                                            text = "✕",
                                            color = MaterialTheme.colorScheme.error,
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

@Composable
private fun AddScheduleRuleDialog(
    onConfirm: (String?, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var startText by remember { mutableStateOf("22:00") }
    var endText by remember { mutableStateOf("07:00") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.schedule_rule_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text(stringResource(Res.string.schedule_start_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text(stringResource(Res.string.schedule_end_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(Res.string.schedule_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.schedule_invalid_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = parseHHmmToMinuteOfDay(startText)
                    val end = parseHHmmToMinuteOfDay(endText)
                    if (start == null || end == null) {
                        showError = true
                        return@TextButton
                    }
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
}

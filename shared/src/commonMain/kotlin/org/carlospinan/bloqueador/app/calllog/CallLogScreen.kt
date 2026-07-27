package org.carlospinan.bloqueador.app.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.call_log_empty_hint
import bloqueallamadas.shared.generated.resources.call_log_title
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.jetbrains.compose.resources.stringResource

@Composable
fun CallLogScreen(
    entries: List<CallLogEntryData>,
    onBack: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.call_log_title),
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
                            CallLogEntryRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallLogEntryRow(entry: CallLogEntryData) {
    val isBlocked = entry.action == "BLOCKED"
    val actionColor = if (isBlocked) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            }
            Text(
                text = entry.action,
                style = MaterialTheme.typography.labelMedium,
                color = actionColor,
            )
        }
    }
}

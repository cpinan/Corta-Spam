package org.carlospinan.bloqueador.app.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.stats_empty
import bloqueallamadas.shared.generated.resources.stats_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.rules.DayStat
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsScreen(
    state: StatsUiState,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.stats_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isLoading) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (state.dailyStats.isEmpty() || state.dailyStats.all { it.count == 0 }) {
                    Text(
                        text = stringResource(Res.string.stats_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(state.dailyStats, key = { it.dateLabel }) { day ->
                            DayStatCard(day)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStatCard(day: DayStat) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = day.dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${day.count} blocked",
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (day.count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

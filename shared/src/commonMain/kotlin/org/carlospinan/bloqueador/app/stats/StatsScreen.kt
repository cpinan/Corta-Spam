package org.carlospinan.bloqueador.app.stats

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
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.stats_blocked_label
import cortaspam.shared.generated.resources.stats_day_n_days_ago
import cortaspam.shared.generated.resources.stats_day_today
import cortaspam.shared.generated.resources.stats_day_yesterday
import cortaspam.shared.generated.resources.stats_empty
import cortaspam.shared.generated.resources.stats_loading
import cortaspam.shared.generated.resources.stats_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.rules.DayStat
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsScreen(
    state: StatsUiState,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()

    CortaSpamTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.stats_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isLoading) {
                    Text(
                        text = stringResource(Res.string.stats_loading),
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
                        items(state.dailyStats, key = { it.daysAgo }) { day ->
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
                text = dayLabel(day.daysAgo),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${day.count} ${stringResource(Res.string.stats_blocked_label)}",
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (day.count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * "Today" / "Yesterday" / "N days ago" for a bucket [daysAgo] days back.
 *
 * Lives here, not in the repository, because only a Composable can reach `stringResource` --
 * the repository used to hardcode the English words for all four shipped locales.
 */
@Composable
private fun dayLabel(daysAgo: Int): String =
    when (daysAgo) {
        0 -> stringResource(Res.string.stats_day_today)
        1 -> stringResource(Res.string.stats_day_yesterday)
        else -> stringResource(Res.string.stats_day_n_days_ago, daysAgo)
    }

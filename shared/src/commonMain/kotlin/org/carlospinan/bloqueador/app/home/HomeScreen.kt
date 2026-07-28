package org.carlospinan.bloqueador.app.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.home_blocked_this_month
import bloqueallamadas.shared.generated.resources.home_blocked_this_week
import bloqueallamadas.shared.generated.resources.home_blocked_today
import bloqueallamadas.shared.generated.resources.home_manage_block_list
import bloqueallamadas.shared.generated.resources.home_settings
import bloqueallamadas.shared.generated.resources.home_title
import bloqueallamadas.shared.generated.resources.home_view_call_log
import bloqueallamadas.shared.generated.resources.home_view_stats
import bloqueallamadas.shared.generated.resources.ic_block_lists
import bloqueallamadas.shared.generated.resources.ic_call_log
import bloqueallamadas.shared.generated.resources.ic_settings
import bloqueallamadas.shared.generated.resources.ic_stats
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    blockingEnabled: Boolean,
    onNavigateToCallLog: () -> Unit,
    onNavigateToCallLogToday: () -> Unit,
    onNavigateToCallLogThisWeek: () -> Unit,
    onNavigateToCallLogThisMonth: () -> Unit,
    onNavigateToBlockList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(
                windowSizeClass = windowSizeClass,
                contentModifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                val isWide = windowSizeClass != WindowSizeClass.Compact

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            HomeStatsSection(
                                state = state,
                                blockingEnabled = blockingEnabled,
                                onToggleBlocking = onToggleBlocking,
                                onNavigateToCallLogToday = onNavigateToCallLogToday,
                                onNavigateToCallLogThisWeek = onNavigateToCallLogThisWeek,
                                onNavigateToCallLogThisMonth = onNavigateToCallLogThisMonth,
                            )
                        }
                        HomeQuickGrid(
                            onNavigateToCallLog = onNavigateToCallLog,
                            onNavigateToStats = onNavigateToStats,
                            onNavigateToBlockList = onNavigateToBlockList,
                            onNavigateToSettings = onNavigateToSettings,
                        )
                    }
                } else {
                    HomeStatsSection(
                        state = state,
                        blockingEnabled = blockingEnabled,
                        onToggleBlocking = onToggleBlocking,
                        onNavigateToCallLogToday = onNavigateToCallLogToday,
                        onNavigateToCallLogThisWeek = onNavigateToCallLogThisWeek,
                        onNavigateToCallLogThisMonth = onNavigateToCallLogThisMonth,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToCallLog) {
                        Text(text = stringResource(Res.string.home_view_call_log))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToStats) {
                        Text(text = stringResource(Res.string.home_view_stats))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToBlockList) {
                        Text(text = stringResource(Res.string.home_manage_block_list))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToSettings) {
                        Text(text = stringResource(Res.string.home_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatsSection(
    state: HomeUiState,
    blockingEnabled: Boolean,
    onToggleBlocking: (Boolean) -> Unit,
    onNavigateToCallLogToday: () -> Unit,
    onNavigateToCallLogThisWeek: () -> Unit,
    onNavigateToCallLogThisMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Switch(
            checked = blockingEnabled,
            onCheckedChange = onToggleBlocking,
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToCallLogToday),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.home_blocked_today),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${state.blockedToday}",
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f).clickable(onClick = onNavigateToCallLogThisWeek),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(Res.string.home_blocked_this_week),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${state.blockedThisWeek}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f).clickable(onClick = onNavigateToCallLogThisMonth),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(Res.string.home_blocked_this_month),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${state.blockedThisMonth}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeQuickGrid(
    onNavigateToCallLog: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToBlockList: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(modifier = Modifier.width(220.dp)) {
        FlowRow(
            maxItemsInEachRow = 2,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeQuickItem(
                title = stringResource(Res.string.home_view_call_log),
                icon = Res.drawable.ic_call_log,
                onClick = onNavigateToCallLog,
            )
            HomeQuickItem(
                title = stringResource(Res.string.home_view_stats),
                icon = Res.drawable.ic_stats,
                onClick = onNavigateToStats,
            )
            HomeQuickItem(
                title = stringResource(Res.string.home_manage_block_list),
                icon = Res.drawable.ic_block_lists,
                onClick = onNavigateToBlockList,
            )
            HomeQuickItem(
                title = stringResource(Res.string.home_settings),
                icon = Res.drawable.ic_settings,
                onClick = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun HomeQuickItem(
    title: String,
    icon: DrawableResource,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

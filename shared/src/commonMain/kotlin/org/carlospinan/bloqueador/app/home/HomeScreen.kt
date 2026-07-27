package org.carlospinan.bloqueador.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigateToCallLog: () -> Unit,
    onNavigateToBlockList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.weight(1f),
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
                        modifier = Modifier.weight(1f),
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

                Spacer(modifier = Modifier.height(32.dp))

                androidx.compose.material3.TextButton(onClick = onNavigateToCallLog) {
                    Text(text = stringResource(Res.string.home_view_call_log))
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

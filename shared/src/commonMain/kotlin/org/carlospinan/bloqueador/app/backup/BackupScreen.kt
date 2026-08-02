package org.carlospinan.bloqueador.app.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.action_ok
import bloqueallamadas.shared.generated.resources.backup_example_body
import bloqueallamadas.shared.generated.resources.backup_example_title
import bloqueallamadas.shared.generated.resources.backup_export
import bloqueallamadas.shared.generated.resources.backup_import
import bloqueallamadas.shared.generated.resources.backup_title
import bloqueallamadas.shared.generated.resources.backup_view_example
import kotlinx.coroutines.flow.Flow
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.stringResource

/** Trimmed BackupData shape, shown verbatim so hand-edited import files match the real format. */
private const val EXAMPLE_JSON = """{
  "version": 1,
  "exportedAt": 1730000000000,
  "blockedNumbers": [
    { "number": "+15551234567", "label": "Robocaller", "createdAt": 1730000000000 }
  ],
  "allowlistedNumbers": [
    { "number": "+15559876543", "label": "Mom's new number", "createdAt": 1730000000000 }
  ],
  "patternRules": [
    { "pattern": "+1900*", "label": "Premium-rate prefix", "enabled": true, "createdAt": 1730000000000 }
  ],
  "countryRules": [],
  "actionRules": [],
  "scheduleRules": []
}"""

@Composable
fun BackupScreen(
    effect: Flow<BackupEffect>,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExampleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(effect) {
        effect.collect { result ->
            val message =
                when (result) {
                    is BackupEffect.Success -> result.message
                    is BackupEffect.Failure -> result.message
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AdaptiveContent(windowSizeClass = windowSizeClass) {
                    Text(
                        text = stringResource(Res.string.backup_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.backup_export))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.backup_import))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showExampleDialog = true }) {
                        Text(stringResource(Res.string.backup_view_example))
                    }
                }
            }
        }
    }

    if (showExampleDialog) {
        AlertDialog(
            onDismissRequest = { showExampleDialog = false },
            title = { Text(stringResource(Res.string.backup_example_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(Res.string.backup_example_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = EXAMPLE_JSON,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExampleDialog = false }) {
                    Text(stringResource(Res.string.action_ok))
                }
            },
        )
    }
}

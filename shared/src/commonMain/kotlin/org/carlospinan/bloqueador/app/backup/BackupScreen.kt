package org.carlospinan.bloqueador.app.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.backup_export
import bloqueallamadas.shared.generated.resources.backup_import
import bloqueallamadas.shared.generated.resources.backup_title
import kotlinx.coroutines.flow.Flow
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupScreen(
    effect: Flow<BackupEffect>,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

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
                }
            }
        }
    }
}

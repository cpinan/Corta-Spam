package org.carlospinan.bloqueador.app.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.backup_export
import bloqueallamadas.shared.generated.resources.backup_import
import bloqueallamadas.shared.generated.resources.backup_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupScreen(
    state: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
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

                if (state.message != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (state.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                    )
                }
            }
        }
    }
}

package org.carlospinan.bloqueador.app.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.settings_tutorial_body
import cortaspam.shared.generated.resources.settings_tutorial_dismiss
import cortaspam.shared.generated.resources.settings_tutorial_open
import cortaspam.shared.generated.resources.settings_tutorial_title
import org.jetbrains.compose.resources.stringResource

/**
 * The once-only walkthrough of Settings shown on first launch.
 *
 * Stateless: the caller decides when it is shown and records that it was. Dismissing by tapping
 * outside counts the same as "Got it" -- both call [onDismiss].
 */
@Composable
fun SettingsTutorialDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_tutorial_title)) },
        text = {
            Text(
                stringResource(Res.string.settings_tutorial_body),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text(stringResource(Res.string.settings_tutorial_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.settings_tutorial_dismiss)) }
        },
    )
}

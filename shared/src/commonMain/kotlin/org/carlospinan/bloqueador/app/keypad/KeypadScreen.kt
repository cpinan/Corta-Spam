package org.carlospinan.bloqueador.app.keypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.keypad_call
import cortaspam.shared.generated.resources.keypad_delete
import cortaspam.shared.generated.resources.keypad_hint
import cortaspam.shared.generated.resources.keypad_title
import org.carlospinan.bloqueador.app.adaptive.ScrollableScreenColumn
import org.jetbrains.compose.resources.stringResource

/**
 * The dial pad. This app takes `ROLE_DIALER`, which means the platform dialer stops being the
 * user's way to place a call -- without this screen, becoming the default phone app removed the
 * ability to make one.
 *
 * [dialRequest] is what an `ACTION_DIAL` intent carried. The screen deliberately does *not* place
 * that call itself: `ACTION_DIAL` means "show this number, let the user decide", unlike
 * `ACTION_CALL`. Dialling it automatically would turn every `tel:` link on the web into a call
 * placed without a confirmation.
 */
@Composable
fun KeypadScreen(
    dialRequest: DialRequest? = null,
    onCall: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var typed by rememberSaveable { mutableStateOf("") }

    // Applied at most once per request. Keying the effect on the request alone is not enough:
    // navigating away and back builds a fresh effect, which would retype a number the user had
    // already deleted. Keying on the id it last applied is what makes "again" different from
    // "still the same request".
    var appliedRequestId by rememberSaveable { mutableStateOf(NO_REQUEST_APPLIED) }
    LaunchedEffect(dialRequest) {
        val request = dialRequest ?: return@LaunchedEffect
        if (request.id == appliedRequestId) return@LaunchedEffect
        typed = request.number
        appliedRequestId = request.id
    }

    MaterialTheme {
        Surface(modifier = modifier.fillMaxWidth()) {
            ScrollableScreenColumn(
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.keypad_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = typed.ifEmpty { stringResource(Res.string.keypad_hint) },
                    style = if (typed.isEmpty()) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineMedium,
                    color =
                        if (typed.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                KEY_ROWS.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { key ->
                            OutlinedButton(
                                onClick = { typed += key.digit },
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            ) {
                                Text(
                                    text = key.digit,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // '+' sits here rather than behind a long-press on 0: a long-press is
                    // undiscoverable, and without any way to type it no international number
                    // could be dialled from this screen at all.
                    OutlinedButton(
                        onClick = { typed += "+" },
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(text = "+", style = MaterialTheme.typography.titleLarge)
                    }

                    val deleteLabel = stringResource(Res.string.keypad_delete)
                    TextButton(
                        onClick = { typed = typed.dropLast(1) },
                        enabled = typed.isNotEmpty(),
                        modifier = Modifier.semantics { contentDescription = deleteLabel },
                    ) {
                        Text(text = "⌫")
                    }

                    Button(
                        onClick = { onCall(typed) },
                        enabled = typed.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text(text = stringResource(Res.string.keypad_call))
                    }
                }
            }
        }
    }
}

private const val NO_REQUEST_APPLIED = -1L

private data class KeypadKey(
    val digit: String,
)

private val KEY_ROWS: List<List<KeypadKey>> =
    listOf(
        listOf(KeypadKey("1"), KeypadKey("2"), KeypadKey("3")),
        listOf(KeypadKey("4"), KeypadKey("5"), KeypadKey("6")),
        listOf(KeypadKey("7"), KeypadKey("8"), KeypadKey("9")),
        listOf(KeypadKey("*"), KeypadKey("0"), KeypadKey("#")),
    )

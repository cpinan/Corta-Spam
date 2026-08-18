package org.carlospinan.bloqueador.app.keypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The twelve-key grid every phone has had since 1963, shared by the dialer screen and the in-call
 * DTMF pad.
 *
 * One component rather than two so the keys cannot end up in different places on the two screens:
 * someone who has learnt where `#` is while dialling must not have to hunt for it again while an
 * automated menu counts down at them.
 *
 * `+` is deliberately *not* here. It can be dialled but has no DTMF tone, so it belongs to the
 * dialer screen alone — see KeypadScreen, which puts it beside delete and call.
 */
@Composable
fun DialPad(
    onKey: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DIAL_PAD_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Text(
                            text = key.toString(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private val DIAL_PAD_ROWS: List<List<Char>> =
    listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#'),
    )

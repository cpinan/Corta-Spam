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
import androidx.compose.ui.unit.Dp
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
 *
 * [keyHeight] and [rowSpacing] are how the dialer screen fills a tall phone: the pad grows into
 * the space rather than leaving it blank above itself. They default to the compact values the
 * in-call DTMF pad wants, where the pad shares the screen with the call's own controls and must
 * not push them anywhere.
 */
@Composable
fun DialPad(
    onKey: (Char) -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = MIN_KEY_HEIGHT,
    rowSpacing: Dp = 8.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DIAL_PAD_ROWS.forEachIndexed { index, row ->
            if (index > 0) Spacer(modifier = Modifier.height(rowSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = { onKey(key) },
                        // heightIn rather than height: a key may never be smaller than Material's
                        // comfortable touch target, whatever a caller computes, and a font scale
                        // that needs more room is allowed to have it.
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(min = keyHeight.coerceAtLeast(MIN_KEY_HEIGHT)),
                    ) {
                        Text(
                            text = key.toString(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }
}

/** Never smaller than this, whoever is asking. */
private val MIN_KEY_HEIGHT = 56.dp

private val DIAL_PAD_ROWS: List<List<Char>> =
    listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#'),
    )

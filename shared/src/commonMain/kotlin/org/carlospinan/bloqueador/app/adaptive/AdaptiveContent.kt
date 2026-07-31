package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveContent(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (windowSizeClass) {
        WindowSizeClass.Compact -> {
            Column(
                modifier = modifier.fillMaxWidth().padding(24.dp).then(contentModifier),
                content = content,
            )
        }
        WindowSizeClass.Medium,
        WindowSizeClass.Expanded,
        -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(24.dp)
                            .then(contentModifier),
                    content = content,
                )
            }
        }
    }
}

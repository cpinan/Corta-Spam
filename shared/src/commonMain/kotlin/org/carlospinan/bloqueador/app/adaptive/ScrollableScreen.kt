package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A full-screen column that scrolls when its content is taller than the window, and still honours
 * [verticalArrangement] when it is not.
 *
 * The naive fix for a clipped screen is to add `verticalScroll` to the existing column. That works
 * for top-aligned content and silently breaks centred content: `verticalScroll` measures its child
 * with unbounded height, so the column wraps its content, there is no spare space left, and
 * [Arrangement.Center] becomes a no-op. Every centred screen would quietly become top-aligned.
 * Giving the column a minimum height of the viewport keeps the spare space when content is short,
 * so centring still works, while letting it grow and scroll when content is tall.
 *
 * Written after a real dead end: on a 1344x2992 emulator at font scale 1.8, the default-dialer
 * explainer clipped its own Continue and Not now buttons off the bottom of a non-scrolling column.
 * There was no way to proceed and no way to scroll — first-run onboarding could not be completed
 * at all at that font size.
 *
 * [safeDrawingPadding] is applied to the viewport rather than inside the scroll, so the scrollable
 * area itself stops short of the status and navigation bars. The app draws edge to edge, and
 * content that scrolls under a system bar and stays there is the same bug in a different costume.
 */
@Composable
fun ScrollableScreenColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

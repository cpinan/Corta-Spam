package org.carlospinan.bloqueador.app.credits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.credits_empty
import cortaspam.shared.generated.resources.credits_intro
import cortaspam.shared.generated.resources.settings_credits_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.stringResource

/**
 * Lists everyone in [CONTRIBUTORS], or says names are on the way while that list is empty.
 *
 * [contributors] is a parameter rather than a direct read of the top-level list so a test can
 * render both states; production passes the real one from the nav host.
 */
@Composable
fun CreditsScreen(
    contributors: List<Contributor> = CONTRIBUTORS,
    onBack: () -> Unit,
) {
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(
                windowSizeClass = windowSizeClass,
                contentModifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.settings_credits_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.credits_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (contributors.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.credits_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    contributors.forEach { contributor ->
                        ContributorCard(contributor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributorCard(contributor: Contributor) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            contributor.contribution?.let { contribution ->
                Text(
                    text = contribution,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

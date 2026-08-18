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
import cortaspam.shared.generated.resources.credits_open_source_intro
import cortaspam.shared.generated.resources.credits_open_source_title
import cortaspam.shared.generated.resources.settings_credits_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.stringResource

/**
 * Lists everyone in [CONTRIBUTORS], then the libraries in [OPEN_SOURCE_COMPONENTS].
 *
 * Two sections rather than one list because the two are owed for different reasons: a person is
 * credited as a courtesy, and an Apache-2.0 or MIT library is credited because its licence asks
 * to be. Flattening them would let a licence obligation read as a thank-you note.
 *
 * [contributors] and [components] are parameters rather than direct reads of the top-level lists
 * so a test can render the empty state as well as the populated one; production passes the real
 * ones from the nav host.
 */
@Composable
fun CreditsScreen(
    contributors: List<Contributor> = CONTRIBUTORS,
    components: List<OpenSourceComponent> = OPEN_SOURCE_COMPONENTS,
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

                if (components.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.credits_open_source_title),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.credits_open_source_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    components.forEach { component ->
                        OpenSourceCard(component)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Name, SPDX licence and project home. The URL is rendered as plain text rather than a link:
 * this screen is reachable while the app holds the dialer role, and a tap that leaves for a
 * browser mid-call is not what anyone came here for. Text can still be read and typed.
 */
@Composable
private fun OpenSourceCard(component: OpenSourceComponent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = component.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = component.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = component.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

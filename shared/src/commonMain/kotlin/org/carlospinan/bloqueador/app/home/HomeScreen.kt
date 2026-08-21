package org.carlospinan.bloqueador.app.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.home_blocked_this_month
import cortaspam.shared.generated.resources.home_blocked_this_week
import cortaspam.shared.generated.resources.home_blocked_today
import cortaspam.shared.generated.resources.home_blocking_disabled
import cortaspam.shared.generated.resources.home_blocking_enabled
import cortaspam.shared.generated.resources.home_blocking_inert
import cortaspam.shared.generated.resources.home_call_in_progress
import cortaspam.shared.generated.resources.home_manage_block_list
import cortaspam.shared.generated.resources.home_pending_review
import cortaspam.shared.generated.resources.home_return_to_call
import cortaspam.shared.generated.resources.home_settings
import cortaspam.shared.generated.resources.home_title
import cortaspam.shared.generated.resources.home_view_call_log
import cortaspam.shared.generated.resources.home_view_stats
import cortaspam.shared.generated.resources.ic_block_lists
import cortaspam.shared.generated.resources.ic_call_log
import cortaspam.shared.generated.resources.ic_settings
import cortaspam.shared.generated.resources.ic_stats
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.permissions.PermissionWarnings
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigateToCallLog: () -> Unit,
    onNavigateToCallLogToday: () -> Unit,
    onNavigateToCallLogThisWeek: () -> Unit,
    onNavigateToCallLogThisMonth: () -> Unit,
    onNavigateToCallLogReview: () -> Unit,
    onNavigateToBlockList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    dialerRoleHeld: Boolean = true,
    notificationsPermissionGranted: Boolean = true,
    fullScreenIntentAllowed: Boolean = true,
    callPhonePermissionGranted: Boolean = true,
    onRequestDialerRole: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenFullScreenIntentSettings: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    /** Whether a call is up right now, so Home can offer the way back to it. */
    callInProgress: Boolean = false,
    onReturnToCall: () -> Unit = {},
) {
    val windowSizeClass = rememberWindowSizeClass()

    CortaSpamTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(
                windowSizeClass = windowSizeClass,
                contentModifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                val isWide = windowSizeClass != WindowSizeClass.Compact

                // Above the stats deliberately: a user whose blocking has stopped working needs
                // to see why before they read a "0 blocked today" that looks like good news.
                // Capped at one card -- a fresh install that granted nothing produced four, which
                // pushed the toggle and every counter below the fold on a 1080x2640 phone.
                // Above even the permission warnings: a live call outranks configuration advice,
                // and this card is the only route back to the call screen for a user who has
                // turned notifications off and pressed Home.
                if (callInProgress) {
                    ReturnToCallCard(onClick = onReturnToCall)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                PermissionWarnings(
                    modifier = Modifier.fillMaxWidth(),
                    dialerRoleHeld = dialerRoleHeld,
                    notificationsPermissionGranted = notificationsPermissionGranted,
                    fullScreenIntentAllowed = fullScreenIntentAllowed,
                    callPhonePermissionGranted = callPhonePermissionGranted,
                    limit = 1,
                    onRequestDialerRole = onRequestDialerRole,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onSeeAll = onNavigateToSettings,
                )

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            HomeStatsSection(
                                state = state,
                                dialerRoleHeld = dialerRoleHeld,
                                onNavigateToSettings = onNavigateToSettings,
                                onToggleBlocking = onToggleBlocking,
                                onNavigateToCallLogToday = onNavigateToCallLogToday,
                                onNavigateToCallLogThisWeek = onNavigateToCallLogThisWeek,
                                onNavigateToCallLogThisMonth = onNavigateToCallLogThisMonth,
                                onNavigateToCallLogReview = onNavigateToCallLogReview,
                            )
                        }
                        HomeQuickGrid(
                            onNavigateToCallLog = onNavigateToCallLog,
                            onNavigateToStats = onNavigateToStats,
                            onNavigateToBlockList = onNavigateToBlockList,
                            onNavigateToSettings = onNavigateToSettings,
                        )
                    }
                } else {
                    HomeStatsSection(
                        state = state,
                        dialerRoleHeld = dialerRoleHeld,
                        onNavigateToSettings = onNavigateToSettings,
                        onToggleBlocking = onToggleBlocking,
                        onNavigateToCallLogToday = onNavigateToCallLogToday,
                        onNavigateToCallLogThisWeek = onNavigateToCallLogThisWeek,
                        onNavigateToCallLogThisMonth = onNavigateToCallLogThisMonth,
                        onNavigateToCallLogReview = onNavigateToCallLogReview,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToCallLog) {
                        Text(text = stringResource(Res.string.home_view_call_log))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToStats) {
                        Text(text = stringResource(Res.string.home_view_stats))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToBlockList) {
                        Text(text = stringResource(Res.string.home_manage_block_list))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToSettings) {
                        Text(text = stringResource(Res.string.home_settings))
                    }
                }
            }
        }
    }
}

/**
 * The way back to a call the user has left. Deliberately not gated on notifications being on:
 * the ongoing-call notification is the other route back, and switching notifications off must not
 * take away the only one.
 */
@Composable
private fun ReturnToCallCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.home_call_in_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(Res.string.home_return_to_call),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun HomeStatsSection(
    state: HomeUiState,
    dialerRoleHeld: Boolean,
    onNavigateToSettings: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onNavigateToCallLogToday: () -> Unit,
    onNavigateToCallLogThisWeek: () -> Unit,
    onNavigateToCallLogThisMonth: () -> Unit,
    onNavigateToCallLogReview: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Settings is not on the navigation bar -- Material's five slots went to Home,
            // Keypad, Agenda, Log and Lists -- so this is its entry point. It has to be *here*
            // rather than only in the text links at the bottom of this screen: those sit below
            // the fold on a phone, which the suite states outright ("last quick link is off
            // screen but reachable by scrolling"). A destination whose only route is off screen
            // is a destination the user has to already know about.
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(Res.drawable.ic_settings),
                    contentDescription = stringResource(Res.string.home_settings),
                )
            }
            Switch(
                checked = state.blockingEnabled,
                onCheckedChange = onToggleBlocking,
            )
        }
    }

    // The switch reports the *setting*; without the dialer role no call ever reaches the app to
    // be filtered, so saying "blocked calls are filtered" there would be a flat lie sitting
    // directly under a banner saying screening is off.
    Text(
        text =
            stringResource(
                when {
                    !state.blockingEnabled -> Res.string.home_blocking_disabled
                    !dialerRoleHeld -> Res.string.home_blocking_inert
                    else -> Res.string.home_blocking_enabled
                },
            ),
        style = MaterialTheme.typography.bodySmall,
        color =
            if (state.blockingEnabled && !dialerRoleHeld) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )

    Spacer(modifier = Modifier.height(32.dp))

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToCallLogToday),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.home_blocked_today),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${state.blockedToday}",
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f).clickable(onClick = onNavigateToCallLogThisWeek),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(Res.string.home_blocked_this_week),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${state.blockedThisWeek}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f).clickable(onClick = onNavigateToCallLogThisMonth),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(Res.string.home_blocked_this_month),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${state.blockedThisMonth}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }

    if (state.pendingReview > 0) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToCallLogReview),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.home_pending_review),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${state.pendingReview}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeQuickGrid(
    onNavigateToCallLog: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToBlockList: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(modifier = Modifier.width(220.dp)) {
        FlowRow(
            maxItemsInEachRow = 2,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeQuickItem(
                title = stringResource(Res.string.home_view_call_log),
                icon = Res.drawable.ic_call_log,
                onClick = onNavigateToCallLog,
            )
            HomeQuickItem(
                title = stringResource(Res.string.home_view_stats),
                icon = Res.drawable.ic_stats,
                onClick = onNavigateToStats,
            )
            HomeQuickItem(
                title = stringResource(Res.string.home_manage_block_list),
                icon = Res.drawable.ic_block_lists,
                onClick = onNavigateToBlockList,
            )
            HomeQuickItem(
                title = stringResource(Res.string.home_settings),
                icon = Res.drawable.ic_settings,
                onClick = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun HomeQuickItem(
    title: String,
    icon: DrawableResource,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.ic_block_lists
import cortaspam.shared.generated.resources.ic_call_log
import cortaspam.shared.generated.resources.ic_contacts
import cortaspam.shared.generated.resources.ic_home
import cortaspam.shared.generated.resources.ic_keypad
import cortaspam.shared.generated.resources.nav_agenda
import cortaspam.shared.generated.resources.nav_home
import cortaspam.shared.generated.resources.nav_keypad
import cortaspam.shared.generated.resources.nav_lists
import cortaspam.shared.generated.resources.nav_log
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class NavItem(
    val label: String,
    val icon: DrawableResource,
    val contentDescription: String,
)

@Composable
fun AdaptiveScaffold(
    windowSizeClass: WindowSizeClass,
    selectedIndex: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val navItems =
        listOf(
            NavItem(stringResource(Res.string.nav_home), Res.drawable.ic_home, stringResource(Res.string.nav_home)),
            NavItem(stringResource(Res.string.nav_keypad), Res.drawable.ic_keypad, stringResource(Res.string.nav_keypad)),
            NavItem(stringResource(Res.string.nav_agenda), Res.drawable.ic_contacts, stringResource(Res.string.nav_agenda)),
            NavItem(stringResource(Res.string.nav_log), Res.drawable.ic_call_log, stringResource(Res.string.nav_log)),
            NavItem(stringResource(Res.string.nav_lists), Res.drawable.ic_block_lists, stringResource(Res.string.nav_lists)),
        )

    // The bar and the rail are chrome, not a screen, and nothing else themes them.
    //
    // Every screen opens CortaSpamTheme itself -- it has to, because InCallActivity draws
    // CallScreen outside the nav host and each screen test renders one screen with no host at
    // all. That made per-screen theming look sufficient, and CortaSpamThemeTest enforces it by
    // hunting for `MaterialTheme { }`. This file opened no theme at all, so it was never an
    // offender by that rule and inherited Material 3's baseline light palette from the
    // composition root: with the system in dark mode the content went dark and the navigation bar
    // underneath it stayed white. Seen on a Pixel 8 Pro API 36 emulator, release build.
    //
    // Nesting the same theme inside itself where a screen opens its own is harmless -- it
    // resolves to identical values -- and keeps both entry points correct on their own.
    CortaSpamTheme {
        when (windowSizeClass) {
            WindowSizeClass.Compact -> {
                Column(modifier = modifier.fillMaxSize()) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                                    ),
                                ),
                    ) {
                        content()
                    }
                    NavigationBar {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = { onNavigate(index) },
                                icon = {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        contentDescription = item.contentDescription,
                                    )
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
            WindowSizeClass.Medium -> {
                Row(modifier = modifier.fillMaxSize()) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedIndex == index,
                                onClick = { onNavigate(index) },
                                icon = {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        contentDescription = item.contentDescription,
                                    )
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Top + WindowInsetsSides.Right + WindowInsetsSides.Bottom,
                                    ),
                                ),
                    ) {
                        content()
                    }
                }
            }
            WindowSizeClass.Expanded -> {
                Row(modifier = modifier.fillMaxSize()) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedIndex == index,
                                onClick = { onNavigate(index) },
                                icon = {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        contentDescription = item.contentDescription,
                                    )
                                },
                                label = { Text(item.label) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Top + WindowInsetsSides.Right + WindowInsetsSides.Bottom,
                                    ),
                                ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

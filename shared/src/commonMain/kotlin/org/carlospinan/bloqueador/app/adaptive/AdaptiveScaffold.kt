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
import cortaspam.shared.generated.resources.ic_home
import cortaspam.shared.generated.resources.ic_keypad
import cortaspam.shared.generated.resources.ic_settings
import cortaspam.shared.generated.resources.nav_home
import cortaspam.shared.generated.resources.nav_keypad
import cortaspam.shared.generated.resources.nav_lists
import cortaspam.shared.generated.resources.nav_log
import cortaspam.shared.generated.resources.nav_settings
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
            NavItem(stringResource(Res.string.nav_log), Res.drawable.ic_call_log, stringResource(Res.string.nav_log)),
            NavItem(stringResource(Res.string.nav_lists), Res.drawable.ic_block_lists, stringResource(Res.string.nav_lists)),
            NavItem(stringResource(Res.string.nav_settings), Res.drawable.ic_settings, stringResource(Res.string.nav_settings)),
        )

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

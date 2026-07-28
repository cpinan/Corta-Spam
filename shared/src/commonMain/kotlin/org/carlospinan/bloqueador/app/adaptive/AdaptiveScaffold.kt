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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class NavItem(
    val label: String,
    val icon: ImageVector,
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
            NavItem("Home", Icons.Default.Home, "Home"),
            NavItem("Log", Icons.AutoMirrored.Filled.List, "Call Log"),
            NavItem("Lists", Icons.Default.Lock, "Block Lists"),
            NavItem("Settings", Icons.Default.Settings, "Settings"),
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
                            icon = { Icon(item.icon, contentDescription = item.contentDescription) },
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
                            icon = { Icon(item.icon, contentDescription = item.contentDescription) },
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
                            icon = { Icon(item.icon, contentDescription = item.contentDescription) },
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

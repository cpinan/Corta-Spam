package org.carlospinan.bloqueador.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import org.carlospinan.bloqueador.app.navigation.AppNavHost

@Composable
fun App(
    onPickAudio: (() -> Unit)? = null,
    onRequestContactsPermission: (() -> Unit)? = null,
    onShareFile: ((String) -> Unit)? = null,
    onPickImportFile: (((String) -> Unit) -> Unit)? = null,
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        onPickAudio = onPickAudio,
        onRequestContactsPermission = onRequestContactsPermission,
        onShareFile = onShareFile,
        onPickImportFile = onPickImportFile,
    )
}

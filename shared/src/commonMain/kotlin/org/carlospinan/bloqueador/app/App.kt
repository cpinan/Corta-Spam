package org.carlospinan.bloqueador.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import org.carlospinan.bloqueador.app.navigation.AppNavHost

@Composable
fun App() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}

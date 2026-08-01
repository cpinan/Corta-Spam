package org.carlospinan.bloqueador.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import org.carlospinan.bloqueador.app.di.initKoin
import platform.UIKit.UIViewController

@Suppress("FunctionName")
@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController {
        App()
    }
}

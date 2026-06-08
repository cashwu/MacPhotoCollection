package org.photocollection

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.photocollection.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mac Photo Collection",
        state = rememberWindowState(width = 1100.dp, height = 720.dp),
    ) {
        App()
    }
}

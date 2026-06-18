package com.ai.assist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.ai.assist.ui.AssistApp
import com.ai.assist.ui.AssistController
import com.ai.assist.ui.theme.AI_assistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val controller = remember { AssistController(applicationContext) }
            DisposableEffect(Unit) {
                onDispose { controller.close() }
            }
            AI_assistTheme(monochrome = controller.settings.monochromeUi) {
                AssistApp(controller = controller)
            }
        }
    }
}

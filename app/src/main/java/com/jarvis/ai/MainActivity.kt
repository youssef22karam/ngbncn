package com.jarvis.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jarvis.ai.ui.theme.JarvisAITheme
import com.jarvis.ai.ui.screens.AppNavigation
import com.jarvis.ai.ui.theme.JarvisBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JarvisAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisBackground
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

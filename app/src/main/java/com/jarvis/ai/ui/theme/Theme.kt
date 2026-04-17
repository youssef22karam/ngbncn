package com.jarvis.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary          = JarvisBlue,
    onPrimary        = JarvisBackground,
    primaryContainer = JarvisNavy,
    onPrimaryContainer = JarvisBlue,
    secondary        = JarvisDarkBlue,
    onSecondary      = JarvisText,
    secondaryContainer = JarvisCardBg,
    onSecondaryContainer = JarvisTextSub,
    tertiary         = JarvisOrange,
    background       = JarvisBackground,
    onBackground     = JarvisText,
    surface          = JarvisNavy,
    onSurface        = JarvisText,
    surfaceVariant   = JarvisCardBg,
    onSurfaceVariant = JarvisTextSub,
    outline          = JarvisDivider,
    error            = JarvisRed,
    onError          = Color.White,
)

@Composable
fun JarvisAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography  = JarvisTypography,
        content     = content
    )
}

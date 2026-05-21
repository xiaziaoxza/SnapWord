package com.snapword.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue500 = Color(0xFF2196F3)
private val Blue700 = Color(0xFF1976D2)
private val Blue200 = Color(0xFF90CAF9)
private val Orange500 = Color(0xFFFF9800)
private val Orange200 = Color(0xFFFFCC80)
private val Teal200 = Color(0xFF00796B)

private val LightColors = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue200,
    secondary = Orange500,
    secondaryContainer = Orange200,
    tertiary = Teal200
)

private val DarkColors = darkColorScheme(
    primary = Blue200,
    onPrimary = Color.Black,
    primaryContainer = Blue700,
    secondary = Orange200,
    secondaryContainer = Orange500,
    tertiary = Teal200
)

@Composable
fun SnapWordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

package com.lockpact.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LockPactDarkColorScheme = darkColorScheme(
    primary = LockPrimary,
    onPrimary = Color(0xFF1D1B15),
    primaryContainer = LockPrimaryDark,
    onPrimaryContainer = LockPrimary,
    secondary = LockMuted,
    onSecondary = LockBackground,
    background = LockBackground,
    onBackground = LockText,
    surface = LockSurface,
    onSurface = LockText,
    surfaceVariant = LockSurfaceHigh,
    onSurfaceVariant = LockMuted,
    outline = LockBorder,
    error = LockAlert,
    onError = Color.White
)

private val LockPactShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun LockPactTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LockPactDarkColorScheme,
        typography = Typography,
        shapes = LockPactShapes,
        content = content
    )
}

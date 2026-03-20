package io.dodge.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DodgeDarkColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = NeonPink,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = Border
)

@Composable
fun DodgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DodgeDarkColors,
        content = content
    )
}

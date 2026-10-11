package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ObsidianNeonColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = ObsidianBlack,
    primaryContainer = NeonMagentaDark,
    onPrimaryContainer = NeonMagentaLight,
    inversePrimary = NeonMagentaLight,
    
    secondary = NeonEmerald,
    onSecondary = ObsidianBlack,
    secondaryContainer = NeonEmeraldDark,
    onSecondaryContainer = NeonEmeraldLight,
    
    tertiary = CyanCyber,
    onTertiary = ObsidianBlack,
    tertiaryContainer = ObsidianSurfaceElevated,
    onTertiaryContainer = CyanCyber,
    
    background = ObsidianBackground,
    onBackground = TextPrimary,
    
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonMagenta,
    
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderGlow,
    
    error = RedAlert,
    onError = ObsidianBlack
)

@Composable
fun VvcMovieStudioTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBlack.toArgb()
                window.navigationBarColor = ObsidianBlack.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ObsidianNeonColorScheme,
        typography = RetroConsoleTypography,
        content = content
    )
}

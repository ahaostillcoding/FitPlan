package com.fitplan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FitPlanGreen = Color(0xFF267A3C)
val FitPlanGreenDark = Color(0xFF0E5A2C)
val FitPlanGreenSoft = Color(0xFFE5F4E9)
val FitPlanBackground = Color(0xFFEEF6F0)
val FitPlanSurface = Color(0xFFFFFFFF)
val FitPlanSurfaceSoft = Color(0xFFF6FBF7)

private val LightColors = lightColorScheme(
    primary = FitPlanGreen,
    onPrimary = Color.White,
    primaryContainer = FitPlanGreenSoft,
    onPrimaryContainer = FitPlanGreenDark,
    secondary = FitPlanGreenDark,
    onSecondary = Color.White,
    secondaryContainer = FitPlanGreenSoft,
    onSecondaryContainer = FitPlanGreenDark,
    background = FitPlanBackground,
    onBackground = Color(0xFF111B14),
    surface = FitPlanSurface,
    onSurface = Color(0xFF111B14),
    surfaceVariant = FitPlanSurfaceSoft,
    onSurfaceVariant = Color(0xFF66756B),
    outline = Color(0xFFD9E7DD),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFEECEB)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD99D),
    onPrimary = Color(0xFF063915),
    primaryContainer = Color(0xFF164E25),
    onPrimaryContainer = Color(0xFFC7F0CE),
    background = Color(0xFF0E1510),
    onBackground = Color(0xFFE6EFE7),
    surface = Color(0xFF131D15),
    onSurface = Color(0xFFE6EFE7),
    surfaceVariant = Color(0xFF203124),
    onSurfaceVariant = Color(0xFFB9C8BC),
    outline = Color(0xFF3F5144)
)

@Composable
fun FitPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

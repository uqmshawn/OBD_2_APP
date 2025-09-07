package com.hurtec.obd2.diagnostics.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Modern Material Design 3 theme for Hurtec OBD-II
 * Supports dynamic colors, dark theme, and automotive-specific colors
 */

private val DarkColorScheme = darkColorScheme(
    primary = HurtecBlue80,
    onPrimary = HurtecBlue20,
    primaryContainer = HurtecBlue30,
    onPrimaryContainer = HurtecBlue90,
    secondary = HurtecBlue80,
    onSecondary = HurtecBlue20,
    secondaryContainer = HurtecBlue30,
    onSecondaryContainer = HurtecBlue90,
    tertiary = HurtecGreen80,
    onTertiary = HurtecGreen20,
    tertiaryContainer = HurtecGreen30,
    onTertiaryContainer = HurtecGreen90,
    error = HurtecRed80,
    onError = HurtecRed20,
    errorContainer = HurtecRed30,
    onErrorContainer = HurtecRed90,
    background = Grey10,
    onBackground = Grey90,
    surface = Grey10,
    onSurface = Grey90,
    surfaceVariant = GreyVariant30,
    onSurfaceVariant = GreyVariant80,
    outline = GreyVariant60
)

private val LightColorScheme = lightColorScheme(
    primary = HurtecBlue40,
    onPrimary = HurtecBlue100,
    primaryContainer = HurtecBlue90,
    onPrimaryContainer = HurtecBlue10,
    secondary = HurtecBlue40,
    onSecondary = HurtecBlue100,
    secondaryContainer = HurtecBlue90,
    onSecondaryContainer = HurtecBlue10,
    tertiary = HurtecGreen40,
    onTertiary = HurtecGreen100,
    tertiaryContainer = HurtecGreen90,
    onTertiaryContainer = HurtecGreen10,
    error = HurtecRed40,
    onError = HurtecRed100,
    errorContainer = HurtecRed90,
    onErrorContainer = HurtecRed10,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = GreyVariant90,
    onSurfaceVariant = GreyVariant30,
    outline = GreyVariant50
)

@Composable
fun HurtecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HurtecTypography,
        shapes = HurtecShapes,
        content = content
    )
}

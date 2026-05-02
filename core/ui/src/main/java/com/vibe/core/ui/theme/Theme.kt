package com.vibe.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Светлая цветовая схема в стиле Telegram.
 */
private val LightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = TextOnBlue,
    primaryContainer = TelegramBlueLight,
    onPrimaryContainer = TextOnBlue,
    secondary = TelegramBlue,
    onSecondary = TextOnBlue,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

/**
 * Темная цветовая схема в стиле Telegram.
 */
private val DarkColorScheme = darkColorScheme(
    primary = TelegramBlueLight,
    onPrimary = TextOnBlue,
    primaryContainer = TelegramBlueDark,
    onPrimaryContainer = TextOnBlue,
    secondary = TelegramBlueLight,
    onSecondary = TextOnBlue,
    background = DarkBackgroundPrimary,
    onBackground = DarkTextPrimary,
    surface = DarkBackgroundPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBackgroundSecondary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDividerColor
)

/**
 * Основная тема приложения Vibe Messenger.
 * Поддерживает системную тему и ручной переключатель.
 *
 * @param darkTheme true для темной темы, false для светлой, null для системной
 * @param dynamicColor false (в Telegram нет динамических цветов из обоев)
 * @param content Content композируемый
 */
@Composable
fun VibeMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

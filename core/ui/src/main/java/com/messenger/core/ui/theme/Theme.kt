package com.messenger.core.ui.theme

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
import com.messenger.core.ui.theme.TelegramColors.*

/**
 * Темная цветовая схема в стиле Telegram.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    error = Error,
    onError = Color.White
)

/**
 * Светлая цветовая схема в стиле Telegram.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    surface = Surface,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Error,
    onError = Color.White
)

/**
 * Тема приложения в стиле Telegram.
 * 
 * @param darkTheme true для темной темы, false для светлой, null для системной
 * @param dynamicColor true для использования динамических цветов (Material You)
 * @param content контент Composable
 */
@Composable
fun TelegramTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Определяем текущую тему
    val useDarkTheme = darkTheme ?: isSystemInDarkTheme()
    
    // Выбираем цветовую схему
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    // Настраиваем статус бар и навигационную панель
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TelegramTypography,
        content = content
    )
}

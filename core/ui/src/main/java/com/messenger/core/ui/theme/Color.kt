package com.messenger.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цветовая палитра в стиле Telegram.
 * 
 * Основные цвета адаптированы под Material 3 и поддерживают
 * системную тему с возможностью ручного переключения.
 */
object TelegramColors {
    // Primary colors - основной синий цвет Telegram
    val Primary = Color(0xFF2481CC)
    val PrimaryVariant = Color(0xFF1A6FB3)
    val OnPrimary = Color.White
    
    // Secondary colors - акцентные цвета
    val Secondary = Color(0xFF50A7EA)
    val SecondaryVariant = Color(0xFF3B8BC7)
    val OnSecondary = Color.White
    
    // Background colors - цвета фона
    val Background = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFF0E1621)
    val Surface = Color(0xFFF5F5F5)
    val SurfaceDark = Color(0xFF17212B)
    
    // Chat background
    val ChatBackground = Color(0xFFEAEAEA)
    val ChatBackgroundDark = Color(0xFF0F161C)
    
    // Message bubbles
    val MessageOutgoing = Color(0xFFEEFFDE)
    val MessageOutgoingDark = Color(0xFF182533)
    val MessageIncoming = Color.White
    val MessageIncomingDark = Color(0xFF17212B)
    
    // Text colors
    val TextPrimary = Color(0xFF000000)
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF707579)
    val TextSecondaryDark = Color(0xFFAAAAAA)
    val TextHint = Color(0xFFA8A8A8)
    val TextHintDark = Color(0xFF6D767D)
    
    // Status colors
    val Online = Color(0xFF4CD964)
    val Offline = Color(0xFF707579)
    val Error = Color(0xFFE53935)
    val Warning = Color(0xFFFFA726)
    val Success = Color(0xFF4CAF50)
    
    // Divider
    val Divider = Color(0xFFE3E3E3)
    val DividerDark = Color(0xFF0E1621)
    
    // Icon colors
    val IconPrimary = Color(0xFF707579)
    val IconPrimaryDark = Color(0xFFAAAAAA)
    
    // Message status
    val MessageRead = Color(0xFF4FAE4E)
    val MessageSent = Color(0xFF707579)
    val MessagePending = Color(0xFFAAAAAA)
}

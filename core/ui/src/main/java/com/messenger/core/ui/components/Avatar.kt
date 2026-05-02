package com.messenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.messenger.core.ui.theme.TelegramColors

/**
 * Компонент аватара пользователя в стиле Telegram.
 * 
 * @param imageUrl URL изображения аватара
 * @param name имя пользователя для генерации цвета фона, если нет аватара
 * @param size размер аватара в dp
 * @param onClick обработчик клика
 */
@Composable
fun Avatar(
    imageUrl: String?,
    name: String,
    size: Int = 56,
    onClick: (() -> Unit)? = null
) {
    val avatarSize = size.dp
    val backgroundColor = getColorForName(name)
    
    Box(
        modifier = Modifier
            .size(avatarSize)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Аватар $name",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(name),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Индикатор онлайн статуса.
 * 
 * @param isOnline статус онлайн
 * @param modifier модификатор
 */
@Composable
fun OnlineIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                if (isOnline) TelegramColors.Online else TelegramColors.Offline
            )
    )
}

/**
 * Получает инициалы из имени.
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex())
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

/**
 * Генерирует цвет на основе имени для аватара.
 */
private fun getColorForName(name: String): Color {
    val colors = listOf(
        Color(0xFFE57070), // Красный
        Color(0xFFFFB74D), // Оранжевый
        Color(0xFFF06292), // Розовый
        Color(0xFFBA68C8), // Фиолетовый
        Color(0xFF9575CD), // Глубокий фиолетовый
        Color(0xFF64B5F6), // Голубой
        Color(0xFF4DB6AC), // Бирюзовый
        Color(0xFF81C784), // Зеленый
        Color(0xFFFFD54F), // Желтый
        Color(0xFFA1887F)  // Коричневый
    )
    
    val index = name.hashCode().absoluteValue % colors.size
    return colors[index]
}

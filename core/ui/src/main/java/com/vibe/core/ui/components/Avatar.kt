package com.vibe.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Компонент аватара пользователя в стиле Telegram.
 * Показывает изображение или инициалы на цветном фоне.
 *
 * @param imageUrl URL изображения аватара
 * @param name Имя для отображения инициалов
 * @param size Размер аватара
 * @param modifier Modifier
 */
@Composable
fun Avatar(
    imageUrl: String?,
    name: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    if (!imageUrl.isNullOrEmpty()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Аватар $name",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = getInitials(name)
        val backgroundColor = getNameColor(name)
        
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Получение инициалов из имени.
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
 * Генерация цвета фона на основе имени.
 * Использует хеш строки для детерминированного выбора цвета.
 */
@Composable
private fun getNameColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE53935), // Красный
        Color(0xFFD81B60), // Розовый
        Color(0xFF8E24AA), // Фиолетовый
        Color(0xFF5E35B1), // Индиго
        Color(0xFF3949AB), // Синий
        Color(0xFF1E88E5), // Голубой
        Color(0xFF039BE5), // Светло-синий
        Color(0xFF00ACC1), // Бирюзовый
        Color(0xFF00897B), // Мятный
        Color(0xFF43A047), // Зеленый
        Color(0xFF7CB342), // Салатовый
        Color(0xFFC0CA33), // Лайм
        Color(0xFFFDD835), // Желтый
        Color(0xFFFFB300), // Оранжевый
        Color(0xFFFF6F00)  // Янтарный
    )
    
    val hash = name.hashCode().absoluteValue
    return colors[hash % colors.size]
}

/**
 * Аватар с индикатором онлайна.
 */
@Composable
fun AvatarWithOnlineIndicator(
    imageUrl: String?,
    name: String,
    isOnline: Boolean,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Avatar(
            imageUrl = imageUrl,
            name = name,
            size = size
        )
        
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.25).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4FA847))
            )
        }
    }
}

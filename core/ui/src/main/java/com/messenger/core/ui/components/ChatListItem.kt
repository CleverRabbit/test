package com.messenger.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.messenger.core.ui.theme.TelegramColors

/**
 * Элемент списка чатов в стиле Telegram.
 * 
 * @param avatarUrl URL аватара чата
 * @param title заголовок чата (имя контакта или название группы)
 * @param subtitle последнее сообщение или статус
 * @param time время последнего сообщения
 * @param unreadCount количество непрочитанных сообщений
 * @param isOnline статус онлайн для контакта
 * @param onClick обработчик клика
 * @param modifier модификатор
 */
@Composable
fun ChatListItem(
    avatarUrl: String?,
    title: String,
    subtitle: String,
    time: String,
    unreadCount: Int = 0,
    isOnline: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар с индикатором онлайн
        Box {
            Avatar(
                imageUrl = avatarUrl,
                name = title,
                size = 56
            )
            
            if (isOnline) {
                OnlineIndicator(
                    isOnline = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Информация о чате
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Заголовок и время
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = TelegramColors.TextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Подзаголовок и счетчик непрочитанных
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TelegramColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    UnreadBadge(count = unreadCount)
                }
            }
        }
    }
}

/**
 * Бейдж непрочитанных сообщений.
 */
@Composable
fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(TelegramColors.Primary)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = TelegramColors.OnPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

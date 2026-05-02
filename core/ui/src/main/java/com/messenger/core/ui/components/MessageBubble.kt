package com.messenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.messenger.core.ui.theme.TelegramColors

/**
 * Компонент сообщения в стиле Telegram.
 * 
 * @param text текст сообщения
 * @param isOutgoing true если сообщение исходящее, false если входящее
 * @param time время отправки
 * @param status статус доставки (отправлено, прочитано)
 * @param modifier модификатор
 */
@Composable
fun MessageBubble(
    text: String,
    isOutgoing: Boolean,
    time: String,
    status: MessageStatus = MessageStatus.SENT,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isOutgoing) 12.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = Int.MAX_VALUE
                )
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = TelegramColors.TextSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                    
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status)
                    }
                }
            }
        }
    }
}

/**
 * Статус сообщения.
 */
enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ
}

/**
 * Иконка статуса сообщения.
 */
@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    val color = when (status) {
        MessageStatus.PENDING -> TelegramColors.MessagePending
        MessageStatus.SENT -> TelegramColors.MessageSent
        MessageStatus.DELIVERED -> TelegramColors.MessageRead
        MessageStatus.READ -> TelegramColors.MessageRead
    }
    
    // Временная реализация с текстом, заменить на векторные иконки
    Text(
        text = when (status) {
            MessageStatus.PENDING -> "⏳"
            MessageStatus.SENT -> "✓"
            MessageStatus.DELIVERED -> "✓✓"
            MessageStatus.READ -> "✓✓"
        },
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

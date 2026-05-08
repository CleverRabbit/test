package com.matrix.tapikapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity сообщения для Room database.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    
    val chatId: String,
    
    val senderId: String,
    
    val content: String,
    
    val timestamp: Long,
    
    val status: String = "sending",
    
    val isIncoming: Boolean,
    
    val mediaUrl: String? = null,
    
    val mediaType: String? = null,
    
    val syncedAt: Long = System.currentTimeMillis()
)

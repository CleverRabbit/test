package com.matrix.tapikapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity чата для Room database.
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    val avatarUrl: String? = null,
    
    val lastMessage: String? = null,
    
    val lastMessageTime: Long = 0,
    
    val unreadCount: Int = 0,
    
    val isGroup: Boolean = false,
    
    val participantsCount: Int = 1,
    
    val syncedAt: Long = System.currentTimeMillis()
)

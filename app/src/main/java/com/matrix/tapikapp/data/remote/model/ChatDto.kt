package com.matrix.tapikapp.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO чата для сетевых запросов.
 */
@Serializable
data class ChatDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerialName("last_message")
    val lastMessage: String? = null,
    
    @SerialName("last_message_time")
    val lastMessageTime: Long = 0,
    
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    
    @SerialName("is_group")
    val isGroup: Boolean = false,
    
    @SerialName("participants_count")
    val participantsCount: Int = 1
)

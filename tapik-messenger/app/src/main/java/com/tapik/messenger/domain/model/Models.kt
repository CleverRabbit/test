package com.tapik.messenger.domain.model

data class User(
    val id: String,
    val username: String,
    val phoneNumber: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false
)

data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String?,
    val lastMessageTime: Long?,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null,
    val isGroup: Boolean = false,
    val membersCount: Int = 1
)

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

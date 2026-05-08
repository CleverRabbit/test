package com.matrix.tapikapp.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO сообщения для сетевых запросов.
 */
@Serializable
data class MessageDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("chat_id")
    val chatId: String,
    
    @SerialName("sender_id")
    val senderId: String,
    
    @SerialName("content")
    val content: String,
    
    @SerialName("timestamp")
    val timestamp: Long,
    
    @SerialName("status")
    val status: String = "sending",
    
    @SerialName("is_incoming")
    val isIncoming: Boolean,
    
    @SerialName("media_url")
    val mediaUrl: String? = null,
    
    @SerialName("media_type")
    val mediaType: String? = null
)

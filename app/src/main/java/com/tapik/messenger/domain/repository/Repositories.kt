package com.tapik.messenger.domain.repository

import com.tapik.messenger.domain.model.Chat
import com.tapik.messenger.domain.model.Message
import com.tapik.messenger.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(phoneNumber: String, code: String): Result<Unit>
    suspend fun logout()
    suspend fun updateProfile(user: User): Result<Unit>
}

interface ChatRepository {
    val chats: Flow<List<Chat>>
    suspend fun getChatById(chatId: String): Flow<Chat?>
    suspend fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun markAsRead(chatId: String): Result<Unit>
    suspend fun deleteChat(chatId: String): Result<Unit>
    suspend fun searchChats(query: String): Flow<List<Chat>>
}

package com.tapik.messenger.data.repository.impl

import com.tapik.messenger.domain.model.Chat
import com.tapik.messenger.domain.model.Message
import com.tapik.messenger.domain.model.User
import com.tapik.messenger.domain.repository.AuthRepository
import com.tapik.messenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(phoneNumber: String, code: String): Result<Unit> {
        // Mock implementation - empty data when no backend
        return Result.success(Unit)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        _currentUser.value = user
        return Result.success(Unit)
    }
}

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    override val chats: Flow<List<Chat>> = _chats.asStateFlow()

    override suspend fun getChatById(chatId: String): Flow<Chat?> = flowOf(null)

    override suspend fun getMessages(chatId: String): Flow<List<Message>> = flowOf(emptyList())

    override suspend fun sendMessage(message: Message): Result<Unit> = Result.success(Unit)

    override suspend fun markAsRead(chatId: String): Result<Unit> = Result.success(Unit)

    override suspend fun deleteChat(chatId: String): Result<Unit> = Result.success(Unit)

    override suspend fun searchChats(query: String): Flow<List<Chat>> = flowOf(emptyList())
}

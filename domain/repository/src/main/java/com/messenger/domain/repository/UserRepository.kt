package com.messenger.domain.repository

import com.messenger.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с пользователями.
 * 
 * Определяет контракт для получения и обновления данных пользователей.
 * Реализация находится в data слое.
 */
interface UserRepository {
    
    /**
     * Поток текущего авторизованного пользователя.
     * Эмитит null если пользователь не авторизован.
     */
    fun getCurrentUser(): Flow<User?>
    
    /**
     * Получает пользователя по ID.
     * 
     * @param userId идентификатор пользователя
     * @return данные пользователя или null если не найден
     */
    suspend fun getUserById(userId: String): User?
    
    /**
     * Получает список всех контактов пользователя.
     * 
     * @return поток со списком контактов
     */
    fun getContacts(): Flow<List<User>>
    
    /**
     * Обновляет профиль текущего пользователя.
     * 
     * @param name новое имя
     * @param avatarUrl URL нового аватара
     * @param bio информация о себе
     */
    suspend fun updateProfile(
        name: String,
        avatarUrl: String? = null,
        bio: String? = null
    ): Result<Unit>
    
    /**
     * Устанавливает статус онлайн/офлайн.
     * 
     * @param isOnline статус онлайн
     */
    suspend fun setOnlineStatus(isOnline: Boolean)
    
    /**
     * Ищет пользователей по запросу.
     * 
     * @param query поисковый запрос
     * @return список найденных пользователей
     */
    suspend fun searchUsers(query: String): List<User>
}

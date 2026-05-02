package com.vibe.domain.repository

import com.vibe.domain.model.user.User
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для операций аутентификации.
 * Интерфейс в доменном слое - определяет контракт без деталей реализации.
 */
interface AuthRepository {
    
    /**
     * Поток текущего авторизованного пользователя.
     * @return Flow с User или null если не авторизован
     */
    val currentUser: Flow<User?>
    
    /**
     * Проверка, авторизован ли пользователь.
     * @return true если авторизован
     */
    fun isAuthorized(): Boolean
    
    /**
     * Вход по номеру телефона.
     * @param phoneNumber Номер телефона
     * @return Result с кодом подтверждения или ошибкой
     */
    suspend fun loginWithPhone(phoneNumber: String): Result<String>
    
    /**
     * Подтверждение кода из SMS.
     * @param phoneNumber Номер телефона
     * @param code Код подтверждения
     * @return Result с пользователем или ошибкой
     */
    suspend fun verifyCode(phoneNumber: String, code: String): Result<User>
    
    /**
     * Выход из аккаунта.
     */
    suspend fun logout()
    
    /**
     * Получение текущего пользователя (синхронно).
     * @return User или null
     */
    fun getCurrentUserSync(): User?
}

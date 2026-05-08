package com.matrix.tapikapp.domain.repository

import com.matrix.tapikapp.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с аутентификацией.
 * 
 * Определяет контракты для входа, регистрации и управления сессией.
 */
interface AuthRepository {

    /**
     * Поток состояния аутентификации текущего пользователя.
     * 
     * @return Flow текущего пользователя или null если не авторизован
     */
    fun getCurrentUserFlow(): Flow<User?>

    /**
     * Отправка кода подтверждения на номер телефона.
     * 
     * @param phone Номер телефона в международном формате
     * @throws ApiException при ошибке сети или сервера
     */
    suspend fun sendVerificationCode(phone: String)

    /**
     * Подтверждение кода и вход в систему.
     * 
     * @param phone Номер телефона
     * @param code Код подтверждения
     * @return Авторизованный пользователь
     * @throws ApiException при неверном коде
     */
    suspend fun verifyCodeAndLogin(phone: String, code: String): User

    /**
     * Выход из системы.
     * 
     * Очищает локальные данные и токены сессии.
     */
    suspend fun logout()

    /**
     * Проверка наличия активной сессии.
     * 
     * @return true если пользователь авторизован
     */
    suspend fun isLoggedIn(): Boolean

    /**
     * Получение текущего пользователя (синхронно).
     * 
     * @return Текущий пользователь или null
     */
    suspend fun getCurrentUser(): User?
}

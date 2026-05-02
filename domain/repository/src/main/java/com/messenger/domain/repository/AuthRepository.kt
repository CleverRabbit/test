package com.messenger.domain.repository

/**
 * Репозиторий для работы с аутентификацией.
 * 
 * Определяет контракт для входа, регистрации и управления сессией.
 * Реализация находится в data слое.
 */
interface AuthRepository {
    
    /**
     * Проверяет, авторизован ли пользователь.
     * 
     * @return true если пользователь авторизован
     */
    suspend fun isAuthorized(): Boolean
    
    /**
     * Выполняет вход по номеру телефона.
     * 
     * @param phoneNumber номер телефона в международном формате
     * @param code код подтверждения из SMS
     * @return Result с токеном сессии или ошибкой
     */
    suspend fun loginWithPhone(
        phoneNumber: String,
        code: String
    ): Result<String>
    
    /**
     * Запрашивает код подтверждения по SMS.
     * 
     * @param phoneNumber номер телефона в международном формате
     * @return Result с идентификатором запроса или ошибкой
     */
    suspend fun requestSmsCode(phoneNumber: String): Result<String>
    
    /**
     * Выполняет выход из аккаунта.
     * Очищает локальные данные и сессию.
     */
    suspend fun logout()
    
    /**
     * Обновляет токен сессии.
     * Вызывается автоматически при истечении токена.
     * 
     * @return Result с новым токеном или ошибкой
     */
    suspend fun refreshToken(): Result<String>
    
    /**
     * Получает текущий токен сессии.
     * 
     * @return токен или null если не авторизован
     */
    suspend fun getToken(): String?
}

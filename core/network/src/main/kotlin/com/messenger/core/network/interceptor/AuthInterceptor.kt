package com.messenger.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Интерцептор для добавления заголовков авторизации.
 * 
 * Автоматически добавляет токен доступа ко всем запросам,
 * если он установлен. Также поддерживает refresh token.
 *
 * @param tokenProvider Поставщик токенов.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_TOKEN_TYPE = "Bearer"
        private const val HEADER_REFRESH_TOKEN = "X-Refresh-Token"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Пропускаем запросы аутентификации (чтобы избежать циклического вызова)
        if (isAuthRequest(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenProvider.getAccessToken()
        val refreshToken = tokenProvider.getRefreshToken()

        // Если нет токена, просто выполняем запрос
        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        // Добавляем заголовок авторизации
        val authenticatedRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, "$HEADER_TOKEN_TYPE $accessToken")
            .apply {
                // Добавляем refresh token при необходимости
                if (!refreshToken.isNullOrBlank()) {
                    header(HEADER_REFRESH_TOKEN, refreshToken)
                }
            }
            .build()

        return try {
            chain.proceed(authenticatedRequest)
        } catch (e: UnauthorizedException) {
            // Обработка случая истёкшего токена
            handleTokenExpiration(chain, originalRequest, e)
        }
    }

    /**
     * Проверяет, является ли запрос запросом аутентификации.
     */
    private fun isAuthRequest(request: okhttp3.Request): Boolean {
        val url = request.url.toString()
        return url.contains("/auth/") || 
               url.contains("/login") || 
               url.contains("/register") ||
               url.contains("/token") ||
               url.contains("/refresh")
    }

    /**
     * Обрабатывает истечение токена.
     * Пытается обновить токен и повторить запрос.
     */
    private suspend fun handleTokenExpiration(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        exception: UnauthorizedException
    ): Response {
        // Пытаемся обновить токен
        val newToken = tokenProvider.refreshAccessToken()
        
        if (newToken != null) {
            // Повторяем запрос с новым токеном
            val newRequest = originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$HEADER_TOKEN_TYPE $newToken")
                .build()
            
            return chain.proceed(newRequest)
        } else {
            // Не удалось обновить токен - пробрасываем исключение
            throw exception
        }
    }
}

/**
 * Поставщик токенов для авторизации.
 * Реализуется в модуле data или feature/auth.
 */
interface TokenProvider {
    
    /**
     * Возвращает текущий access token.
     */
    fun getAccessToken(): String?
    
    /**
     * Возвращает refresh token.
     */
    fun getRefreshToken(): String?
    
    /**
     * Обновляет access token используя refresh token.
     * Возвращает новый токен или null, если обновление не удалось.
     */
    suspend fun refreshAccessToken(): String?
    
    /**
     * Очищает сохранённые токены (при выходе пользователя).
     */
    fun clearTokens()
}

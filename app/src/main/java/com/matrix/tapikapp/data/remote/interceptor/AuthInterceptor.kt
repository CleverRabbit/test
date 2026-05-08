package com.matrix.tapikapp.data.remote.interceptor

import com.matrix.tapikapp.util.logger.AppLogger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для добавления заголовков авторизации.
 * 
 * Автоматически добавляет Bearer токен ко всем запросам,
 * если пользователь авторизован.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    companion object {
        private const val TAG = "Auth"
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Пропускаем запросы аутентификации без токена
        if (shouldBypassAuth(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider.getAccessToken()
        
        if (token.isNullOrBlank()) {
            AppLogger.w(TAG, "Запрос без токена: ${originalRequest.url}")
            return chain.proceed(originalRequest)
        }

        // Добавляем заголовок авторизации
        val authenticatedRequest = originalRequest.newBuilder()
            .header(AUTH_HEADER, "$BEARER_PREFIX$token")
            .build()

        AppLogger.d(TAG, "Добавлен токен авторизации для: ${originalRequest.url}")
        
        return chain.proceed(authenticatedRequest)
    }

    /**
     * Определяет, нужно ли пропустить добавление токена.
     */
    private fun shouldBypassAuth(request: okhttp3.Request): Boolean {
        val url = request.url.toString()
        // Пропускаем запросы аутентификации
        return url.contains("/auth/send-code") || 
               url.contains("/auth/verify-code") ||
               url.contains("/auth/logout")
    }
}

/**
 * Интерфейс для получения токена.
 * Реализуется в DataStoreRepository или SessionManager.
 */
interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
}

package com.messenger.core.network.interceptor

import android.util.Log
import com.messenger.core.network.model.ApiError
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Интерцептор для обработки HTTP статусов и реализации retry-логики.
 * 
 * Обрабатывает:
 * - Повторные попытки при временных ошибках сети
 * - Экспоненциальную задержку между попытками
 * - Кастомную реакцию на HTTP статусы
 * - Логирование с фильтрацией чувствительных данных
 *
 * @param maxRetryCount Максимальное количество попыток.
 * @param initialDelayMs Начальная задержка в миллисекундах.
 * @param maxDelayMs Максимальная задержка в миллисекундах.
 */
class RetryInterceptor(
    private val maxRetryCount: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 30000L
) : Interceptor {

    companion object {
        private const val TAG = "RetryInterceptor"
        
        // HTTP статусы, при которых имеет смысл повторять запрос
        private val RETRYABLE_STATUS_CODES = setOf(
            408, // Request Timeout
            429, // Too Many Requests
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504  // Gateway Timeout
        )
        
        // Статусы, требующие специальной обработки
        private const val STATUS_UNAUTHORIZED = 401
        private const val STATUS_FORBIDDEN = 403
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        var retryCount = 0
        var currentDelay = initialDelayMs

        while (retryCount <= maxRetryCount) {
            try {
                Log.d(TAG, "Выполнение запроса: ${request.method} ${filterSensitiveUrl(request.url.toString())} (попытка ${retryCount + 1}/${maxRetryCount + 1})")
                
                response = chain.proceed(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Запрос успешен: ${response.code}")
                    return response
                }
                
                // Обработка специфичных статусов
                when (response.code) {
                    STATUS_UNAUTHORIZED -> {
                        Log.w(TAG, "Ошибка авторизации (401). Требуется повторный вход.")
                        // Здесь можно вызвать событие для разлогинивания пользователя
                        response.close()
                        throw UnauthorizedException("Сессия истекла или недействительна")
                    }
                    
                    STATUS_FORBIDDEN -> {
                        Log.w(TAG, "Доступ запрещён (403)")
                        response.close()
                        throw ForbiddenException("Доступ к ресурсу запрещён")
                    }
                    
                    in RETRYABLE_STATUS_CODES -> {
                        Log.w(TAG, "Временная ошибка сервера: ${response.code}. Попытка $retryCount/$maxRetryCount")
                        response.close()
                        
                        if (retryCount < maxRetryCount) {
                            delayWithBackoff(currentDelay)
                            retryCount++
                            currentDelay = calculateNextDelay(currentDelay)
                            continue
                        }
                        
                        throw ServerException("Сервер недоступен после $maxRetryCount попыток")
                    }
                    
                    else -> {
                        Log.e(TAG, "HTTP ошибка: ${response.code}")
                        // Для других ошибок возвращаем ответ, чтобы обработчик мог распарсить тело ошибки
                        return response
                    }
                }
                
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "Таймаут соединения: ${e.message}")
                lastException = e
                
                if (retryCount < maxRetryCount) {
                    delayWithBackoff(currentDelay)
                    retryCount++
                    currentDelay = calculateNextDelay(currentDelay)
                    continue
                }
                
                throw TimeoutException("Превышено время ожидания ответа от сервера", e)
                
            } catch (e: IOException) {
                Log.w(TAG, "Ошибка сети: ${e.message}")
                lastException = e
                
                // Проверяем, является ли ошибка временной
                if (isNetworkErrorTemporary(e) && retryCount < maxRetryCount) {
                    delayWithBackoff(currentDelay)
                    retryCount++
                    currentDelay = calculateNextDelay(currentDelay)
                    continue
                }
                
                throw NetworkException("Ошибка сети: ${e.message}", e)
            }
        }

        // Если все попытки исчерпаны
        throw lastException ?: IOException("Неизвестная ошибка после всех попыток")
    }

    /**
     * Вычисляет следующую задержку с экспоненциальным ростом.
     */
    private fun calculateNextDelay(currentDelay: Long): Long {
        return minOf(currentDelay * 2, maxDelayMs)
    }

    /**
     * Асинхронная задержка с учётом backoff.
     */
    private suspend fun delayWithBackoff(delayMs: Long) {
        try {
            delay(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Поток прерван во время задержки", e)
        }
    }

    /**
     * Проверяет, является ли ошибка сети временной.
     */
    private fun isNetworkErrorTemporary(e: IOException): Boolean {
        val message = e.message?.lowercase() ?: return false
        return message.contains("connection") || 
               message.contains("timeout") || 
               message.contains("reset") ||
               message.contains("unreachable")
    }

    /**
     * Фильтрует чувствительные данные из URL для логирования.
     */
    private fun filterSensitiveUrl(url: String): String {
        return url
            .replace(Regex("(token|password|secret|key|auth)=([^&]+)"), "$1=***")
            .replace(Regex("(Bearer\\s+)(\\S+)"), "$1***")
    }
}

/**
 * Исключение для ошибок авторизации.
 */
class UnauthorizedException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Исключение для ошибок доступа.
 */
class ForbiddenException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Исключение для временных ошибок сервера.
 */
class ServerException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Исключение для таймаутов.
 */
class TimeoutException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Исключение для общих ошибок сети.
 */
class NetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

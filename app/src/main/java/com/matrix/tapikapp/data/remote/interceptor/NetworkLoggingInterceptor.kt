package com.matrix.tapikapp.data.remote.interceptor

import com.matrix.tapikapp.util.logger.AppLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Interceptor для обработки HTTP ответов, таймаутов и retry.
 * 
 * Особенности:
 * - Логирование запросов/ответов с маскировкой чувствительных данных
 * - Обработка сетевых ошибок (таймауты, отсутствие сети)
 * - Кастомная реакция на HTTP статусы
 * - Retry logic для определенных статусов
 */
class NetworkLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "Network"
        
        // Статусы для автоматического retry
        private val RETRYABLE_STATUS_CODES = setOf(502, 503, 504)
        
        // Максимальное количество retry
        private const val MAX_RETRY_COUNT = 3
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        
        var tryCount = 0
        var response: Response? = null
        var lastException: IOException? = null

        while (tryCount < MAX_RETRY_COUNT) {
            try {
                val startTime = System.currentTimeMillis()
                response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime

                // Логирование ответа
                logResponse(method, url, response.code, duration, response)

                // Проверка на retryable статусы
                if (response.code in RETRYABLE_STATUS_CODES && tryCount < MAX_RETRY_COUNT - 1) {
                    AppLogger.w(TAG, "Получен статус ${response.code}, попытка retry (${tryCount + 1}/$MAX_RETRY_COUNT)")
                    response.close()
                    tryCount++
                    continue
                }

                return response

            } catch (e: UnknownHostException) {
                lastException = e
                AppLogger.e(TAG, "Нет подключения к интернету", e)
                throw NoInternetException("Отсутствует подключение к сети", e)
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (tryCount < MAX_RETRY_COUNT - 1) {
                    AppLogger.w(TAG, "Таймаут соединения, попытка retry (${tryCount + 1}/$MAX_RETRY_COUNT)")
                    tryCount++
                    continue
                }
                AppLogger.e(TAG, "Превышено время ожидания", e)
                throw TimeoutException("Превышено время ожидания ответа от сервера", e)
            } catch (e: IOException) {
                lastException = e
                AppLogger.e(TAG, "Сетевая ошибка: ${e.message}", e)
                throw NetworkException("Ошибка сети: ${e.message}", e)
            }
        }

        // Если все retry исчерпаны
        throw lastException ?: NetworkException("Не удалось выполнить запрос после $MAX_RETRY_COUNT попыток")
    }

    private fun logResponse(method: String, url: String, code: Int, duration: Long, response: Response?) {
        val statusText = when (code) {
            in 200..299 -> "OK"
            in 400..499 -> "CLIENT_ERROR"
            in 500..599 -> "SERVER_ERROR"
            else -> "UNKNOWN"
        }

        AppLogger.http(
            tag = TAG,
            method = method,
            url = url,
            status = code,
            body = response?.peekBody(1024)?.string()
        )

        AppLogger.d(TAG, "$method $url - $code ($statusText) за ${duration}мс")
    }
}

/**
 * Исключение отсутствия интернета.
 */
class NoInternetException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Исключение таймаута.
 */
class TimeoutException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Базовое сетевое исключение.
 */
class NetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

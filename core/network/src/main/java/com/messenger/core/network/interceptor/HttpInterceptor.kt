package com.messenger.core.network.interceptor

import com.messenger.core.common.logger.LoggerProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Interceptor для обработки HTTP статусов, логирования и retry логики.
 * Обрабатывает типичные сценарии: таймауты, 4xx/5xx ошибки, отсутствие сети.
 */
class HttpInterceptor(
    private val connectTimeoutMs: Long = 15_000,
    private val readTimeoutMs: Long = 30_000,
    private val writeTimeoutMs: Long = 30_000
) : Interceptor {

    companion object {
        private const val HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key"
        private const val HEADER_REQUEST_ID = "X-Request-ID"
    }

    private val logger = LoggerProvider.logger

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        // Добавляем таймауты для конкретных запросов
        val newChain = chain.withConnectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .withReadTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .withWriteTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)

        // Добавляем заголовки для идемпотентности (если не установлены)
        val enrichedRequest = if (request.header(HEADER_IDEMPOTENCY_KEY) == null) {
            request.newBuilder()
                .addHeader(HEADER_IDEMPOTENCY_KEY, generateIdempotencyKey(request))
                .addHeader(HEADER_REQUEST_ID, java.util.UUID.randomUUID().toString())
                .build()
        } else {
            request
        }

        return try {
            val response = newChain.proceed(enrichedRequest)
            val duration = System.currentTimeMillis() - startTime

            // Логирование успешных ответов
            logger.logHttpEvent(
                method = enrichedRequest.method,
                url = enrichedRequest.url.toString(),
                statusCode = response.code,
                responseBody = response.peekBody(2048).string().takeIf { response.code < 400 },
                durationMs = duration
            )

            // Обработка специфических статусов
            when {
                response.code == 401 -> handleUnauthorized(response, enrichedRequest, newChain)
                response.code == 403 -> handleForbidden(response)
                response.code == 408 || response.code == 429 -> {
                    handleRetryableStatus(response, enrichedRequest, newChain)
                }
                response.code >= 500 -> handleServerError(response)
            }

            response

        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            logger.logHttpEvent(
                method = enrichedRequest.method,
                url = enrichedRequest.url.toString(),
                requestBody = enrichedRequest.body?.toString(),
                durationMs = duration
            )
            throw e
        }
    }

    /**
     * Обработка 401 Unauthorized.
     * Здесь можно добавить логику обновления токена.
     */
    private fun handleUnauthorized(
        response: Response,
        request: okhttp3.Request,
        chain: Interceptor.Chain
    ): Response {
        logger.w("HTTP", "Получен 401 Unauthorized. Требуется обновление токена.")
        // TODO: Добавить логику refresh token при необходимости
        // Пример:
        // if (shouldRefreshToken(request)) {
        //     val newToken = refreshToken()
        //     val newRequest = request.newBuilder()
        //         .header("Authorization", "Bearer $newToken")
        //         .build()
        //     return chain.proceed(newRequest)
        // }
        return response
    }

    /**
     * Обработка 403 Forbidden.
     */
    private fun handleForbidden(response: Response): Response {
        logger.e("HTTP", "Получен 403 Forbidden. Доступ запрещён.")
        return response
    }

    /**
     * Обработка_retryable статусов (408, 429).
     */
    private fun handleRetryableStatus(
        response: Response,
        request: okhttp3.Request,
        chain: Interceptor.Chain
    ): Response {
        val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 1L
        logger.w("HTTP", "Получен ${response.code}. Retry-After: ${retryAfter}s")
        
        // Можно добавить автоматический retry с задержкой
        // Thread.sleep(retryAfter * 1000)
        // return chain.proceed(request)
        
        return response
    }

    /**
     * Обработка ошибок сервера (5xx).
     */
    private fun handleServerError(response: Response): Response {
        logger.e("HTTP", "Получена ошибка сервера: ${response.code}")
        return response
    }

    /**
     * Генерация ключа идемпотентности для POST/PUT запросов.
     */
    private fun generateIdempotencyKey(request: okhttp3.Request): String {
        val method = request.method
        val url = request.url.toString()
        val bodyHash = request.body?.toString()?.hashCode() ?: 0
        return "${method}_${url}_${bodyHash}_${System.currentTimeMillis()}"
    }
}

/**
 * Interceptor для добавления заголовков авторизации.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    private val logger = LoggerProvider.logger

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Не добавляем токен к публичным эндпоинтам
        val skipAuthPaths = listOf("/auth/login", "/auth/register", "/auth/refresh")
        if (skipAuthPaths.any { originalRequest.url.encodedPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider()
        
        if (token.isNullOrBlank()) {
            logger.w("Auth", "Токен отсутствует. Запрос без авторизации.")
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}

/**
 * Interceptor для проверки наличия сети.
 */
class NetworkAvailabilityInterceptor(
    private val isNetworkAvailable: () -> Boolean
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isNetworkAvailable()) {
            throw NoNetworkException("Отсутствует подключение к интернету")
        }
        return chain.proceed(chain.request())
    }
}

/**
 * Исключение при отсутствии сети.
 */
class NoNetworkException(message: String) : IOException(message)

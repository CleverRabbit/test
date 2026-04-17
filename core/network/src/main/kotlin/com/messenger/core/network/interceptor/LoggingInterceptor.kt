package com.messenger.core.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Интерцептор для логирования HTTP запросов и ответов.
 * 
 * Особенности:
 * - Фильтрация чувствительных данных (токены, пароли, секреты)
 * - Структурированное логирование с тегами
 * - Поддержка различных уровней детализации
 * - Безопасное отображение JSON тел
 *
 * @param level Уровень логирования.
 */
class LoggingInterceptor(
    private val level: LogLevel = LogLevel.BODY
) : Interceptor {

    companion object {
        private const val TAG_REQUEST = "HttpLogger_Request"
        private const val TAG_RESPONSE = "HttpLogger_Response"
        private const val MAX_LOG_LENGTH = 4000
        
        // Паттерны для фильтрации чувствительных данных
        private val SENSITIVE_PATTERNS = listOf(
            Pattern.compile("(\"?)(access_token|refresh_token|token|password|secret|key|auth|authorization)(\"?)\\s*[:=]\\s*(\"?)([^\"]+)\\4", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Bearer\\s+([A-Za-z0-9\\-_\\.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\"?)(email|phone)(\"?)\\s*[:=]\\s*(\"?)([^\",}]+)\\4", Pattern.CASE_INSENSITIVE)
        )
        
        private val UTF8 = Charset.forName("UTF-8")
    }

    enum class LogLevel {
        NONE,           // Без логов
        BASIC,          // Только метод и URL
        HEADERS,        // Метод, URL и заголовки
        BODY            // Полное логирование (метод, URL, заголовки, тело)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (level == LogLevel.NONE) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val startTime = System.currentTimeMillis()

        // Логирование запроса
        logRequest(request)

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG_REQUEST, "❌ Ошибка выполнения запроса: ${e.message}")
            throw e
        }

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        // Логирование ответа
        logResponse(response, durationMs)

        return response
    }

    private fun logRequest(request: okhttp3.Request) {
        val method = request.method
        val url = filterSensitiveData(request.url.toString())
        
        when (level) {
            LogLevel.NONE -> return
            
            LogLevel.BASIC -> {
                Log.d(TAG_REQUEST, "➡️ $method $url")
            }
            
            LogLevel.HEADERS, LogLevel.BODY -> {
                val headers = formatHeaders(request.headers)
                Log.d(TAG_REQUEST, "➡️ $method $url\n$headers")
                
                if (level == LogLevel.BODY) {
                    request.body?.let { body ->
                        val buffer = Buffer()
                        body.writeTo(buffer)
                        val bodyString = buffer.readUtf8()
                        Log.d(TAG_REQUEST, "📦 Тело запроса:\n${filterSensitiveData(prettifyJson(bodyString))}")
                    } ?: Log.d(TAG_REQUEST, "📦 Тело запроса: пустое")
                }
            }
        }
    }

    private fun logResponse(response: Response, durationMs: Long) {
        val code = response.code
        val message = response.message
        val url = filterSensitiveData(response.request.url.toString())
        
        when (level) {
            LogLevel.NONE -> return
            
            LogLevel.BASIC -> {
                Log.d(TAG_RESPONSE, "⬅️ $code $message ($durationMs ms) $url")
            }
            
            LogLevel.HEADERS, LogLevel.BODY -> {
                val headers = formatHeaders(response.headers)
                Log.d(TAG_RESPONSE, "⬅️ $code $message ($durationMs ms)\n$url\n$headers")
                
                if (level == LogLevel.BODY) {
                    response.body?.let { body ->
                        try {
                            val source = body.source()
                            source.request(Long.MAX_VALUE)
                            val buffer = source.buffer.clone()
                            val bodyString = buffer.readUtf8()
                            Log.d(TAG_RESPONSE, "📦 Тело ответа:\n${filterSensitiveData(prettifyJson(bodyString))}")
                        } catch (e: IOException) {
                            Log.w(TAG_RESPONSE, "Не удалось прочитать тело ответа: ${e.message}")
                        }
                    } ?: Log.d(TAG_RESPONSE, "📦 Тело ответа: пустое")
                }
            }
        }
    }

    private fun formatHeaders(headers: okhttp3.Headers): String {
        return headers.entries
            .joinToString("\n") { "${it.first}: ${filterSensitiveData(it.second)}" }
            .prependIndent("📋 Заголовки:\n")
    }

    /**
     * Фильтрует чувствительные данные из строки.
     */
    private fun filterSensitiveData(input: String): String {
        var result = input
        SENSITIVE_PATTERNS.forEach { pattern ->
            result = pattern.matcher(result).replaceAll("$1$2$3: ***REDACTED***")
        }
        return result
    }

    /**
     * Форматирует JSON для удобного чтения.
     */
    private fun prettifyJson(json: String): String {
        return try {
            // Простая форматировка JSON с отступами
            json.replace("\\{".toRegex(), "{\n  ")
                .replace("\\}".toRegex(), "\n}")
                .replace(",".toRegex(), ",\n  ")
                .take(MAX_LOG_LENGTH) + if (json.length > MAX_LOG_LENGTH) "... (обрезано)" else ""
        } catch (e: Exception) {
            json
        }
    }
}

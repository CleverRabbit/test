package com.messenger.core.common.logger

import mu.KLogger
import mu.KotlinLogging

/**
 * Интерфейс для структурированного логирования с фильтрацией чувствительных данных.
 * Обеспечивает единую точку конфигурации логов для всего приложения.
 */
interface StructuredLogger {
    /**
     * Логирование отладочного сообщения.
     * @param tag тег лога
     * @param message сообщение
     * @param throwable исключение (опционально)
     */
    fun d(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Логирование информационного сообщения.
     * @param tag тег лога
     * @param message сообщение
     * @param throwable исключение (опционально)
     */
    fun i(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Логирование предупреждения.
     * @param tag тег лога
     * @param message сообщение
     * @param throwable исключение (опционально)
     */
    fun w(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Логирование ошибки.
     * @param tag тег лога
     * @param message сообщение
     * @param throwable исключение (опционально)
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Логирование HTTP-запроса/ответа с фильтрацией чувствительных полей.
     * @param method HTTP метод
     * @param url URL запроса
     * @param statusCode код статуса (для ответов)
     * @param requestBody тело запроса (может содержать чувствительные данные)
     * @param responseBody тело ответа (может содержать чувствительные данные)
     * @param durationMs длительность запроса в мс
     */
    fun logHttpEvent(
        method: String,
        url: String,
        statusCode: Int? = null,
        requestBody: String? = null,
        responseBody: String? = null,
        durationMs: Long? = null
    )
}

/**
 * Реализация структурированного логгера с фильтрацией чувствительных данных.
 */
class AppLogger : StructuredLogger {

    companion object {
        private val SENSITIVE_FIELDS = setOf(
            "password", "token", "secret", "key", "auth",
            "authorization", "cookie", "session", "credit_card",
            "cvv", "pin", "ssn"
        )

        private const val REDACTED_VALUE = "***REDACTED***"
        private const val MAX_LOG_LENGTH = 4000
    }

    private val logger: KLogger = KotlinLogging.logger {}

    override fun d(tag: String, message: String, throwable: Throwable?) {
        logger.debug { "[$tag] $message" }
        throwable?.let { logger.debug(it) { "[$tag] Exception" } }
    }

    override fun i(tag: String, message: String, throwable: Throwable?) {
        logger.info { "[$tag] $message" }
        throwable?.let { logger.info(it) { "[$tag] Exception" } }
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        logger.warn { "[$tag] $message" }
        throwable?.let { logger.warn(it) { "[$tag] Exception" } }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        logger.error { "[$tag] $message" }
        throwable?.let { logger.error(it) { "[$tag] Exception" } }
    }

    override fun logHttpEvent(
        method: String,
        url: String,
        statusCode: Int?,
        requestBody: String?,
        responseBody: String?,
        durationMs: Long?
    ) {
        val statusInfo = statusCode?.let { "Status: $it" } ?: ""
        val durationInfo = durationMs?.let { "Duration: ${it}ms" } ?: ""

        val safeRequestBody = requestBody?.let { filterSensitiveData(it) }
        val safeResponseBody = responseBody?.let { filterSensitiveData(it) }

        val logMessage = buildString {
            appendLine("HTTP $method $url")
            if (statusInfo.isNotEmpty()) appendLine(statusInfo)
            if (durationInfo.isNotEmpty()) appendLine(durationInfo)

            safeRequestBody?.let {
                appendLine("Request Body: ${truncateIfNeeded(it)}")
            }

            safeResponseBody?.let {
                appendLine("Response Body: ${truncateIfNeeded(it)}")
            }
        }

        when {
            statusCode == null -> d("HTTP", logMessage)
            statusCode in 200..299 -> i("HTTP", logMessage)
            statusCode in 400..499 -> w("HTTP", logMessage)
            else -> e("HTTP", logMessage)
        }
    }

    /**
     * Фильтрация чувствительных данных из JSON/XML строки.
     */
    private fun filterSensitiveData(data: String): String {
        var result = data
        SENSITIVE_FIELDS.forEach { field ->
            // Простая эвристика для JSON полей
            result = result.replace(
                Regex("(\"$field\"\\s*:\\s*)\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "\$1\"$REDACTED_VALUE\""
            )
            // Для числовых значений
            result = result.replace(
                Regex("(\"$field\"\\s*:\\s*)\\d+", RegexOption.IGNORE_CASE),
                "\$1$REDACTED_VALUE"
            )
        }
        return result
    }

    /**
     * Обрезка длинных строк для логов.
     */
    private fun truncateIfNeeded(text: String, maxLength: Int = MAX_LOG_LENGTH): String {
        return if (text.length > maxLength) {
            text.take(maxLength) + "... [truncated]"
        } else {
            text
        }
    }
}

/**
 * Singleton экземпляр логгера для использования в приложении.
 */
object LoggerProvider {
    val logger: StructuredLogger by lazy { AppLogger() }
}

/**
 * Extension функции для удобного логирования.
 */
fun Any.logD(message: String, throwable: Throwable? = null) {
    LoggerProvider.logger.d(this::class.java.simpleName, message, throwable)
}

fun Any.logI(message: String, throwable: Throwable? = null) {
    LoggerProvider.logger.i(this::class.java.simpleName, message, throwable)
}

fun Any.logW(message: String, throwable: Throwable? = null) {
    LoggerProvider.logger.w(this::class.java.simpleName, message, throwable)
}

fun Any.logE(message: String, throwable: Throwable? = null) {
    LoggerProvider.logger.e(this::class.java.simpleName, message, throwable)
}

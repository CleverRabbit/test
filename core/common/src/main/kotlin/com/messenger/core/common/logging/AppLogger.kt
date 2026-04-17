package com.messenger.core.common.logging

import android.util.Log

/**
 * Централизованная система логирования приложения.
 * 
 * Обеспечивает:
 * - Единый формат логов с префиксом приложения
 * - Разные уровни логирования (Verbose, Debug, Info, Warn, Error)
 * - Фильтрацию чувствительных данных
 * - Возможность отключения в production
 *
 * Использование:
 * ```kotlin
 * AppLogger.d("MyTag", "Сообщение")
 * AppLogger.e("MyTag", "Ошибка", exception)
 * AppLogger.i("MyTag", { "Ленивое вычисление сообщения" })
 * ```
 */
object AppLogger {
    
    private const val TAG_PREFIX = "Messenger_"
    
    /**
     * Флаг включения логирования.
     * В production рекомендуется устанавливать в false.
     */
    var enabled: Boolean = true
    
    /**
     * Минимальный уровень логирования.
     * Сообщения с уровнем ниже не будут записаны.
     */
    var minLevel: LogLevel = LogLevel.DEBUG
    
    /**
     * Логирование уровня VERBOSE.
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }
    
    /**
     * Логирование уровня DEBUG.
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    /**
     * Логирование уровня INFO.
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    /**
     * Логирование уровня WARN.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    /**
     * Логирование уровня ERROR.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    /**
     * Логирование с ленивым вычислением сообщения.
     * Сообщение вычисляется только если логирование включено.
     */
    inline fun d(tag: String, messageProducer: () -> String) {
        if (enabled && LogLevel.DEBUG >= minLevel) {
            d(tag, messageProducer())
        }
    }
    
    /**
     * Логирует HTTP запрос с фильтрацией чувствительных данных.
     */
    fun httpRequest(tag: String, method: String, url: String, body: String? = null) {
        if (enabled && LogLevel.DEBUG >= minLevel) {
            val safeUrl = filterSensitiveData(url)
            val safeBody = body?.let { filterSensitiveData(it) }
            d(tag, "➡️ $method $safeUrl\n${safeBody?.let { "📦 $it" } ?: "📦 (пустое тело)"}")
        }
    }
    
    /**
     * Логирует HTTP ответ.
     */
    fun httpResponse(tag: String, code: Int, method: String, url: String, durationMs: Long, body: String? = null) {
        if (enabled && LogLevel.DEBUG >= minLevel) {
            val safeUrl = filterSensitiveData(url)
            val emoji = when {
                code in 200..299 -> "✅"
                code in 300..399 -> "🔄"
                code in 400..499 -> "❌"
                code >= 500 -> "💥"
                else -> "❓"
            }
            d(tag, "$emoji⬅️ $code $method $safeUrl (${durationMs}ms)\n${body?.let { "📦 ${filterSensitiveData(it)}" } ?: "📦 (пустое тело)"}")
        }
    }
    
    /**
     * Основное сообщение логирования.
     */
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!enabled || level < minLevel) return
        
        val fullTag = "$TAG_PREFIX$tag"
        
        when (level) {
            LogLevel.VERBOSE -> Log.v(fullTag, message, throwable)
            LogLevel.DEBUG -> Log.d(fullTag, message, throwable)
            LogLevel.INFO -> Log.i(fullTag, message, throwable)
            LogLevel.WARN -> Log.w(fullTag, message, throwable)
            LogLevel.ERROR -> Log.e(fullTag, message, throwable)
        }
    }
    
    /**
     * Фильтрует чувствительные данные из строки.
     */
    private fun filterSensitiveData(input: String): String {
        return input
            .replace(Regex("(token|password|secret|key|auth|authorization)=([^&]+)", RegexOption.IGNORE_CASE), "$1=***REDACTED***")
            .replace(Regex("Bearer\\s+([A-Za-z0-9\\-_\\.]+)"), "Bearer ***REDACTED***")
            .replace(Regex("(\"email\"\\s*:\\s*\")([^\"]+)(\")"), "$1***REDACTED***$3")
            .replace(Regex("(\"phone\"\\s*:\\s*\")([^\"]+)(\")"), "$1***REDACTED***$3")
    }
    
    /**
     * Уровни логирования.
     */
    enum class LogLevel(val priority: Int) {
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARN(5),
        ERROR(6);
        
        operator fun compareTo(other: LogLevel): Int = this.priority.compareTo(other.priority)
    }
}

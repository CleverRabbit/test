package com.matrix.tapikapp.util.logger

import android.util.Log
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Централизованный логгер приложения с фильтрацией чувствительных данных.
 * 
 * Обеспечивает:
 * - Структурированное логирование с тегами
 * - Маскировку чувствительных полей (токены, пароли, телефоны)
 * - Форматированный вывод JSON
 * - Разделение логов по уровням важности
 * 
 * Использование:
 * ```
 * AppLogger.d("TAG", "Сообщение")
 * AppLogger.e("TAG", "Ошибка", exception)
 * AppLogger.json("TAG", jsonString)
 * ```
 */
object AppLogger {

    private const val TAG_PREFIX = "TapikApp"
    private val SENSITIVE_FIELDS = setOf(
        "password", "pass", "pwd",
        "token", "access_token", "refresh_token", "auth_token",
        "secret", "api_key", "apikey",
        "phone", "card", "cvv"
    )

    private const val MAX_LOG_LENGTH = 4000
    private const val REDACTED = "[REDACTED]"

    /**
     * Логирование отладочного сообщения.
     */
    fun d(tag: String, message: String) {
        log(Log.DEBUG, tag, message)
    }

    /**
     * Логирование информационного сообщения.
     */
    fun i(tag: String, message: String) {
        log(Log.INFO, tag, message)
    }

    /**
     * Логирование предупреждения.
     */
    fun w(tag: String, message: String) {
        log(Log.WARN, tag, message)
    }

    /**
     * Логирование ошибки.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = buildString {
            append(message)
            throwable?.let {
                append("\n${it.stackTraceToString()}")
            }
        }
        log(Log.ERROR, tag, fullMessage)
    }

    /**
     * Логирование JSON с форматированием и маскировкой чувствительных данных.
     */
    fun json(tag: String, json: String) {
        try {
            val formatted = formatJson(json)
            val sanitized = sanitizeJson(formatted)
            logChunks(Log.DEBUG, tag, sanitized)
        } catch (e: Exception) {
            e(tag, "Ошибка при логировании JSON: ${e.message}")
        }
    }

    /**
     * Логирование HTTP запроса/ответа.
     */
    fun http(tag: String, method: String, url: String, status: Int?, body: String?) {
        val statusStr = status?.let { "[$it]" } ?: "[--]"
        val header = "$method $url $statusStr"
        d(tag, header)
        body?.let { json(tag, it) }
    }

    private fun log(priority: Int, tag: String, message: String) {
        val fullTag = "$TAG_PREFIX/$tag"
        when (priority) {
            Log.DEBUG -> Log.d(fullTag, message)
            Log.INFO -> Log.i(fullTag, message)
            Log.WARN -> Log.w(fullTag, message)
            Log.ERROR -> Log.e(fullTag, message)
        }
    }

    private fun logChunks(priority: Int, tag: String, message: String) {
        if (message.length <= MAX_LOG_LENGTH) {
            log(priority, tag, message)
            return
        }

        var start = 0
        while (start < message.length) {
            val end = minOf(start + MAX_LOG_LENGTH, message.length)
            log(priority, tag, message.substring(start, end))
            start = end
        }
    }

    /**
     * Форматирование JSON для читаемости.
     */
    private fun formatJson(json: String): String {
        // Простая реализация - в production используйте kotlinx.serialization.json.Json
        return json.replace("\\{".toRegex(), "{\n  ")
            .replace("\\}".toRegex(), "\n}")
            .replace(",".toRegex(), ",\n  ")
    }

    /**
     * Маскировка чувствительных данных в JSON.
     */
    private fun sanitizeJson(json: String): String {
        var result = json
        SENSITIVE_FIELDS.forEach { field ->
            val patterns = listOf(
                "\"$field\"\\s*:\\s*\"[^\"]*\"".toRegex(RegexOption.IGNORE_CASE),
                "\"$field\"\\s*:\\s*\\d+".toRegex(RegexOption.IGNORE_CASE)
            )
            patterns.forEach { pattern ->
                result = pattern.replace(result) { match ->
                    val key = match.value.substringBefore(":")
                    "$key: \"$REDACTED\""
                }
            }
        }
        return result
    }
}

package com.messenger.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Базовый ответ API с полями для обработки ошибок.
 * Используйте как обёртку для всех ответов сервера.
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("data")
    val data: T? = null,
    
    @SerialName("error")
    val error: ApiError? = null,
    
    @SerialName("timestamp")
    val timestamp: Long? = null
)

/**
 * Модель ошибки API.
 */
@Serializable
data class ApiError(
    @SerialName("code")
    val code: String,
    
    @SerialName("message")
    val message: String,
    
    @SerialName("details")
    val details: Map<String, String>? = null
)

/**
 * Специальный класс для обработки HTTP статусов.
 */
sealed class NetworkStatus {
    /** Успешный статус */
    object Success : NetworkStatus()
    
    /** Ошибка клиента (4xx) */
    data class ClientError(val code: Int, val message: String) : NetworkStatus()
    
    /** Ошибка сервера (5xx) */
    data class ServerError(val code: Int, val message: String) : NetworkStatus()
    
    /** Сетевая ошибка */
    data class NetworkError(val exception: Throwable) : NetworkStatus()
    
    /** Таймаут */
    object Timeout : NetworkStatus()
    
    /** Нет соединения */
    object NoConnection : NetworkStatus()
}

/**
 * Конвертация HTTP кода статуса в NetworkStatus.
 */
fun Int.toNetworkStatus(errorMessage: String = ""): NetworkStatus {
    return when {
        this in 200..299 -> NetworkStatus.Success
        this in 400..499 -> NetworkStatus.ClientError(this, errorMessage)
        this in 500..599 -> NetworkStatus.ServerError(this, errorMessage)
        else -> NetworkStatus.ServerError(this, errorMessage)
    }
}

package com.matrix.tapikapp.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Универсальный ответ API.
 * 
 * @param T Тип данных ответа
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("data")
    val data: T? = null,
    
    @SerialName("error")
    val error: ApiError? = null
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
    val details: String? = null
)

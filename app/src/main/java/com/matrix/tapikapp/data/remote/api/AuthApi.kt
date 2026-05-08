package com.matrix.tapikapp.data.remote.api

import com.matrix.tapikapp.data.remote.model.ApiResponse
import com.matrix.tapikapp.data.remote.model.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API интерфейс для аутентификации.
 * 
 * Для подключения своего REST API:
 * 1. Измените пути эндпоинтов под вашу структуру
 * 2. Адаптируйте запросы/ответы при необходимости
 */
interface AuthApi {

    /**
     * Отправка кода подтверждения на номер телефона.
     * POST /api/v1/auth/send-code
     */
    @POST("api/v1/auth/send-code")
    suspend fun sendVerificationCode(
        @Body request: SendCodeRequest
    ): ApiResponse<Unit>

    /**
     * Подтверждение кода и получение токена.
     * POST /api/v1/auth/verify-code
     */
    @POST("api/v1/auth/verify-code")
    suspend fun verifyCode(
        @Body request: VerifyCodeRequest
    ): ApiResponse<AuthResponse>

    /**
     * Получение текущего пользователя.
     * GET /api/v1/auth/me
     */
    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>

    /**
     * Выход из системы (инвалидация токена).
     * POST /api/v1/auth/logout
     */
    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}

/**
 * Запрос на отправку кода подтверждения.
 */
@kotlinx.serialization.Serializable
data class SendCodeRequest(
    val phone: String
)

/**
 * Запрос на подтверждение кода.
 */
@kotlinx.serialization.Serializable
data class VerifyCodeRequest(
    val phone: String,
    val code: String
)

/**
 * Ответ аутентификации с токеном.
 */
@kotlinx.serialization.Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserDto
)

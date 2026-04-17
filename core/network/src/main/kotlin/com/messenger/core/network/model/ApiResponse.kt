package com.messenger.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Базовый ответ API.
 * Используется как обёртка для всех ответов сервера.
 *
 * @param T Тип данных ответа.
 * @property success Флаг успешности операции.
 * @property message Сообщение от сервера (опционально).
 * @property data Данные ответа (опционально).
 * @property errorCode Код ошибки (опционально).
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("message")
    val message: String? = null,
    
    @SerialName("data")
    val data: T? = null,
    
    @SerialName("error_code")
    val errorCode: String? = null
)

/**
 * Ошибка API.
 * Содержит информацию об ошибке для отображения пользователю.
 *
 * @property code Код ошибки.
 * @property message Сообщение об ошибке.
 * @property details Детали ошибки (опционально).
 */
@Serializable
data class ApiError(
    @SerialName("code")
    val code: String,
    
    @SerialName("message")
    val message: String,
    
    @SerialName("details")
    val details: Map<String, String>? = null
) {
    companion object {
        const val CODE_NETWORK_ERROR = "NETWORK_ERROR"
        const val CODE_TIMEOUT = "TIMEOUT"
        const val CODE_UNAUTHORIZED = "UNAUTHORIZED"
        const val CODE_FORBIDDEN = "FORBIDDEN"
        const val CODE_NOT_FOUND = "NOT_FOUND"
        const val CODE_SERVER_ERROR = "SERVER_ERROR"
        const val CODE_VALIDATION_ERROR = "VALIDATION_ERROR"
        const val CODE_RATE_LIMITED = "RATE_LIMITED"
    }
}

/**
 * Пагинация для списковых ответов.
 *
 * @property currentPage Текущая страница.
 * @property totalPages Общее количество страниц.
 * @property totalItems Общее количество элементов.
 * @property itemsPerPage Количество элементов на странице.
 * @property hasNext Флаг наличия следующей страницы.
 * @property hasPrevious Флаг наличия предыдущей страницы.
 */
@Serializable
data class Pagination(
    @SerialName("current_page")
    val currentPage: Int,
    
    @SerialName("total_pages")
    val totalPages: Int,
    
    @SerialName("total_items")
    val totalItems: Int,
    
    @SerialName("items_per_page")
    val itemsPerPage: Int,
    
    @SerialName("has_next")
    val hasNext: Boolean = currentPage < totalPages,
    
    @SerialName("has_previous")
    val hasPrevious: Boolean = currentPage > 1
)

/**
 * Ответ с пагинацией.
 *
 * @param T Тип данных элементов.
 * @property data Список элементов.
 * @property pagination Информация о пагинации.
 */
@Serializable
data class PagedResponse<T>(
    @SerialName("data")
    val data: List<T>,
    
    @SerialName("pagination")
    val pagination: Pagination
)

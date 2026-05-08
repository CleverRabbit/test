package com.matrix.tapikapp.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO пользователя для сетевых запросов.
 * 
 * Используется для сериализации/десериализации JSON ответов API.
 * 
 * @property id Уникальный идентификатор
 * @property phone Номер телефона
 * @property firstName Имя
 * @property lastName Фамилия
 * @property avatarUrl URL аватара
 * @property isOnline Статус онлайн
 * @property lastSeen Время последнего посещения (timestamp)
 */
@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("phone")
    val phone: String,
    
    @SerialName("first_name")
    val firstName: String,
    
    @SerialName("last_name")
    val lastName: String? = null,
    
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerialName("is_online")
    val isOnline: Boolean = false,
    
    @SerialName("last_seen")
    val lastSeen: Long? = null
)

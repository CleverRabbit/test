package com.matrix.tapikapp.domain.model

/**
 * Модель пользователя в доменном слое.
 * 
 * @property id Уникальный идентификатор пользователя
 * @property phone Номер телефона
 * @property firstName Имя
 * @property lastName Фамилия (опционально)
 * @property avatarUrl URL аватара (опционально)
 * @property isOnline Статус онлайн
 * @property lastSeen Время последнего посещения
 */
data class User(
    val id: String,
    val phone: String,
    val firstName: String,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
) {
    /**
     * Полное имя пользователя
     */
    val fullName: String
        get() = buildString {
            append(firstName)
            lastName?.let { append(" $it") }
        }
}

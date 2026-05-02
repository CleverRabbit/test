package com.vibe.domain.model.user

/**
 * Модель пользователя в доменном слое.
 *
 * @param id Уникальный идентификатор пользователя
 * @param username Имя пользователя (логин)
 * @param firstName Имя
 * @param lastName Фамилия (опционально)
 * @param phoneNumber Номер телефона
 * @param avatarUrl URL аватара (опционально)
 * @param isOnline Статус онлайн
 * @param lastSeen Время последнего посещения
 */
data class User(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String? = null,
    val phoneNumber: String,
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

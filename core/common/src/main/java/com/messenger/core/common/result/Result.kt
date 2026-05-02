package com.messenger.core.common.result

/**
 *sealed класс для представления результата операции.
 * Обеспечивает типобезопасную обработку успешных результатов и ошибок.
 *
 * @param T тип данных успешного результата
 */
sealed class Result<out T> {
    /**
     * Успешный результат с данными.
     * @param data данные результата
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Результат с ошибкой.
     * @param exception исключение, вызвавшее ошибку
     * @param message сообщение об ошибке (опционально)
     */
    data class Error(
        val exception: Throwable,
        val message: String? = null
    ) : Result<Nothing>()

    /**
     * Проверка на успешность результата.
     * @return true если результат успешен
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Проверка на наличие ошибки.
     * @return true если результат содержит ошибку
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Получение данных или бросание исключения.
     * @return данные результата
     * @throws IllegalStateException если результат содержит ошибку
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Получение данных или значение по умолчанию.
     * @param defaultValue значение по умолчанию
     * @return данные результата или defaultValue
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }

    /**
     * Преобразование успешного результата.
     * @param transform функция трансформации данных
     * @return новый результат с преобразованными данными
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Преобразование успешного результата с обработкой ошибок.
     * @param transform функция трансформации данных
     * @return новый результат с преобразованными данными
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    /**
     * Обработка ошибки.
     * @param action действие для выполнения при ошибке
     * @return тот же результат
     */
    fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) {
            action(exception)
        }
        return this
    }

    /**
     * Выполнение действия при успехе.
     * @param action действие для выполнения при успехе
     * @return тот же результат
     */
    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
}

/**
 * Создание успешного результата.
 */
fun <T> successOf(data: T): Result<T> = Result.Success(data)

/**
 * Создание результата с ошибкой.
 */
fun errorOf(exception: Throwable, message: String? = null): Result<Nothing> =
    Result.Error(exception, message)

/**
 * Создание результата с ошибкой от сообщения.
 */
fun errorOf(message: String): Result<Nothing> =
    Result.Error(Exception(message), message)

/**
 * Безопасное выполнение блока кода с возвратом Result.
 */
inline fun <T> runCatchingResult(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }
}

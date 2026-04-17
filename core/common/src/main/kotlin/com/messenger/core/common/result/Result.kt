package com.messenger.core.common.result

/**
 * Универсальный wrapper для результатов операций.
 * 
 * Используется для представления успешных результатов и ошибок
 * в типобезопасном виде без исключений.
 *
 * @param T Тип успешного значения.
 */
sealed class Result<out T> {
    
    /**
     * Успешный результат.
     *
     * @property data Данные результата.
     */
    data class Success<out T>(val data: T) : Result<T>()
    
    /**
     * Результат с ошибкой.
     *
     * @property exception Исключение, вызвавшее ошибку.
     * @property message Сообщение об ошибке (опционально).
     * @property code Код ошибки (опционально).
     */
    data class Error(
        val exception: Throwable,
        val message: String? = exception.message,
        val code: String? = null
    ) : Result<Nothing>()
    
    /**
     * Проверяет, является ли результат успешным.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Проверяет, является ли результат ошибкой.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Возвращает данные или бросает исключение.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Возвращает данные или бросает исключение.
     *
     * @throws IllegalStateException если результат является ошибкой.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
    
    /**
     * Выполняет действие над успешным результатом.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Выполняет действие над ошибкой.
     */
    inline fun onError(action: (Error) -> Unit): Result<T> {
        if (this is Error) action(this)
        return this
    }
    
    /**
     * Преобразует успешное значение.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    /**
     * Преобразует успешное значение с возможностью возврата Result.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }
    
    /**
     * Возвращает значение по умолчанию при ошибке.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }
    
    companion object {
        /**
         * Создаёт успешный результат.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Создаёт результат с ошибкой.
         */
        fun <T> error(exception: Throwable, message: String? = null, code: String? = null): Result<T> =
            Error(exception, message, code)
        
        /**
         * Выполняет блок кода и оборачивает результат.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}

/**
 * Extension функция для преобразования kotlin.Result в наш Result.
 */
fun <T> kotlin.Result<T>.toAppResult(): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.error(it) }
    )

/**
 * Extension функция для преобразования нашего Result в kotlin.Result.
 */
fun <T> Result<T>.toKotlinResult(): kotlin.Result<T> =
    when (this) {
        is Result.Success -> kotlin.Result.success(data)
        is Result.Error -> kotlin.Result.failure(exception)
    }

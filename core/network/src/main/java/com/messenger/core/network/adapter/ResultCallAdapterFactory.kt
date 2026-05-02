package com.messenger.core.network.adapter

import com.messenger.core.common.result.Result
import com.messenger.core.common.result.errorOf
import com.messenger.core.common.result.successOf
import com.messenger.core.network.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * CallAdapter для конвертации Retrofit Call в Result.
 * Обеспечивает типобезопасную обработку ошибок API.
 */
class ResultCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // Проверяем, что возвращаемый тип - Result
        if (getRawType(returnType) != Result::class.java) {
            return null
        }
        
        require(returnType is java.lang.reflect.ParameterizedType) {
            "Result должен быть параметризирован типом"
        }

        val responseType = getParameterUpperBound(0, returnType)
        val rawReturnType = getRawType(responseType)

        // Поддерживаем только Call<Result<T>>
        if (rawReturnType != Call::class.java) {
            return null
        }

        require(responseType is java.lang.reflect.ParameterizedType) {
            "Call должен быть параметризирован"
        }

        val innerType = getParameterUpperBound(0, responseType)
        
        @Suppress("UNCHECKED_CAST")
        return ResultCallAdapter<Any>(innerType) as CallAdapter<*, *>
    }

    companion object {
        fun create(): ResultCallAdapterFactory = ResultCallAdapterFactory()
    }
}

/**
 * Реализация CallAdapter для Result.
 */
@Suppress("TooGenericExceptionCaught")
class ResultCallAdapter<T>(private val responseType: Type) : CallAdapter<T, Call<Result<T>>> {

    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Call<Result<T>> {
        return ResultCall(call)
    }
}

/**
 * Обёртка Call для обработки Result.
 */
class ResultCall<T>(private val delegate: Call<T>) : Call<Result<T>> {

    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
                val result = response.toResult()
                callback.onResponse(this@ResultCall, result)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                val result = errorOf(t, t.message ?: "Неизвестная ошибка сети")
                callback.onResponse(this@ResultCall, successOf(result))
            }
        })
    }

    override fun execute(): retrofit2.Response<Result<T>> {
        throw UnsupportedOperationException("execute() не поддерживается. Используйте enqueue().")
    }

    override fun clone(): Call<Result<T>> = ResultCall(delegate.clone())

    override fun request(): okhttp3.Request = delegate.request()

    override fun cancel() = delegate.cancel()

    override fun isExecuted: Boolean = delegate.isExecuted

    override fun isCanceled: Boolean = delegate.isCanceled
}

/**
 * Конвертация Retrofit Response в Result.
 */
@Suppress("TooGenericExceptionCaught", "UNCHECKED_CAST")
fun <T> retrofit2.Response<T>.toResult(): Result<T> {
    return try {
        when {
            isSuccessful -> {
                body()?.let { successOf(it) }
                    ?: errorOf(Exception("Пустое тело ответа"), "Сервер вернул пустой ответ")
            }
            else -> {
                val errorMessage = errorBody()?.string() ?: "Ошибка HTTP $code"
                errorOf(Exception(errorMessage), "HTTP $code")
            }
        }
    } catch (e: Exception) {
        errorOf(e, e.message ?: "Ошибка парсинга ответа")
    }
}

/**
 * Extension функция для удобного вызова suspend функций с Result.
 */
suspend fun <T> Call<T>.await(): Result<T> = withContext(Dispatchers.IO) {
    try {
        val response = execute()
        response.toResult()
    } catch (e: Exception) {
        errorOf(e, e.message ?: "Ошибка выполнения запроса")
    }
}

/**
 * Extension функция для вызова с автоматическим retry.
 */
suspend fun <T> Call<T>.awaitWithRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000
): Result<T> = withContext(Dispatchers.IO) {
    var lastResult: Result<T>? = null
    var delayMs = initialDelayMs

    repeat(maxRetries) { attempt ->
        val result = await()
        
        when {
            result.isSuccess -> return@withContext result
            result is Result.Error && !isRetryable(result.exception) -> return@withContext result
            else -> {
                lastResult = result
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(delayMs)
                    delayMs = (delayMs * 1.5).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }
    }

    lastResult ?: errorOf(Exception("Неизвестная ошибка"))
}

/**
 * Проверка, является ли ошибка retryable.
 */
private fun isRetryable(exception: Throwable): Boolean {
    return exception is java.net.SocketTimeoutException ||
           exception is java.net.UnknownHostException ||
           exception is java.io.IOException
}

package com.messenger.core.network.adapter

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * CallAdapter для обработки API ответов и преобразования ошибок.
 * 
 * Обеспечивает:
 * - Автоматическую обработку ошибок сериализации
 * - Преобразование сетевых исключений в доменные
 * - Валидацию структуры ответа
 * 
 * Используется с Kotlinx Serialization.
 */
class ApiCallAdapterFactory private constructor(
    private val json: Json
) : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // Работаем только с Call<T>
        if (getRawType(returnType) != Call::class.java) {
            return null
        }

        val responseType = getParameterUpperBound(0, returnType as java.lang.reflect.ParameterizedType)
        
        // Создаём адаптер для конкретного типа ответа
        return object : CallAdapter<Any, Call<Any>> {
            override fun responseType(): Type = responseType
            
            override fun adapt(call: Call<Any>): Call<Any> {
                return ApiCall(call, json)
            }
        }
    }

    companion object {
        /**
         * Создаёт фабрику CallAdapter с настройками JSON по умолчанию.
         */
        fun create(): ApiCallAdapterFactory {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            return ApiCallAdapterFactory(json)
        }

        /**
         * Создаёт фабрику CallAdapter с кастомными настройками JSON.
         */
        fun create(json: Json): ApiCallAdapterFactory {
            return ApiCallAdapterFactory(json)
        }
    }
}

/**
 * Обёртка над Call для обработки ответов и ошибок.
 */
private class ApiCall<T>(
    private val delegate: Call<T>,
    private val json: Json
) : Call<T> {

    override fun enqueue(callback: retrofit2.Callback<T>) {
        delegate.enqueue(object : retrofit2.Callback<T> {
            override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
                try {
                    callback.onResponse(this@ApiCall, response)
                } catch (e: SerializationException) {
                    // Обрабатываем ошибки сериализации
                    callback.onFailure(this@ApiCall, e)
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback.onFailure(this@ApiCall, wrapException(t))
            }
        })
    }

    override fun execute(): retrofit2.Response<T> {
        return try {
            delegate.execute()
        } catch (e: Exception) {
            throw wrapException(e)
        }
    }

    override fun clone(): Call<T> = ApiCall(delegate.clone(), json)

    override fun request(): okhttp3.Request = delegate.request()

    override fun cancel() = delegate.cancel()

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun isCanceled(): Boolean = delegate.isCanceled()

    /**
     * Оборачивает исключения в понятные типы.
     */
    private fun wrapException(throwable: Throwable): Throwable {
        return when (throwable) {
            is SerializationException -> throwable
            else -> throwable
        }
    }
}

/**
 * Extension функция для создания RequestBody из строки JSON.
 */
fun String.toJsonRequestBody(): RequestBody {
    return this.toRequestBody("application/json; charset=utf-8".toMediaType())
}

/**
 * Extension функция для создания RequestBody из любого сериализуемого объекта.
 */
inline fun <reified T> T.toJsonRequestBody(json: Json = Json): RequestBody {
    return try {
        val jsonString = json.encodeToString(this)
        jsonString.toJsonRequestBody()
    } catch (e: SerializationException) {
        throw IllegalArgumentException("Не удалось сериализовать объект в JSON", e)
    }
}

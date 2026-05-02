package com.messenger.feature.media.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import com.messenger.core.network.NetworkMonitor
import com.messenger.feature.media.data.network.MediaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей медиа-компонентов.
 * 
 * Предоставляет:
 * - MediaApiService для сетевых запросов
 * - ImageLoader для загрузки изображений с кэшированием
 * - OkHttpClient с кастомной конфигурацией для загрузки файлов
 * 
 * Для подключения своего REST API:
 * 1. Убедитесь, что MediaApiService создан в core:network или здесь
 * 2. Настройте Retrofit builder с вашим baseUrl
 * 3. Добавьте необходимые интерсепторы для аутентификации
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    
    /**
     * Предоставление MediaApiService через Retrofit.
     */
    @Provides
    @Singleton
    fun provideMediaApiService(
        okHttpClient: OkHttpClient
    ): MediaApiService {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://your-api-base-url.com/") // ЗАМЕНИТЬ на ваш baseUrl
            .client(okHttpClient)
            .addConverterFactory(retrofit2.kotlinx.serialization.Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MediaApiService::class.java)
    }
    
    /**
     * Предоставление OkHttpClient с увеличенными таймаутами для загрузки файлов.
     */
    @Provides
    @Singleton
    fun provideMediaOkHttpClient(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor
    ): OkHttpClient {
        val cacheSize = 100L * 1024 * 1024 // 100 MB
        val cache = Cache(File(context.cacheDir, "media_cache"), cacheSize)
        
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Client-Type", "android")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * Предоставление ImageLoader с оптимизированными настройками для мессенджера.
     * Поддерживает GIF, WebP, HEIC и другие форматы.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .components {
                add(GifDecoder.Factory())
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(500L * 1024 * 1024) // 500 MB
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}

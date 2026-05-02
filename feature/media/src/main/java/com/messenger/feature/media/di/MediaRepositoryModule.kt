package com.messenger.feature.media.di

import com.messenger.feature.media.data.repository.MediaRepositoryImpl
import com.messenger.feature.media.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt модуль для биндинга репозитория медиа.
 * 
 * Связывает интерфейс MediaRepository с реализацией MediaRepositoryImpl.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaRepositoryModule {
    
    @Binds
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository
}

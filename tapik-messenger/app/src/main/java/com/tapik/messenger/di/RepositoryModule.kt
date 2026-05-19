package com.tapik.messenger.di

import com.tapik.messenger.data.repository.AuthRepositoryImpl
import com.tapik.messenger.data.repository.ChatRepositoryImpl
import com.tapik.messenger.domain.repository.AuthRepository
import com.tapik.messenger.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}

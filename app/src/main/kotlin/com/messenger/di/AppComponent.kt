package com.messenger.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Singleton

/**
 * Главный Hilt компонент приложения.
 * 
 * Автоматически создаётся при использовании @HiltAndroidApp.
 * Предоставляет зависимости для всего приложения.
 */
@Singleton
@Component(
    modules = [
        // Core модули
        // com.messenger.core.network.di.NetworkModule,
        // com.messenger.core.datastore.di.DataStoreModule,
        // com.messenger.core.security.di.SecurityModule,
        
        // Domain модули
        // domain.auth.di.AuthDomainModule,
        // domain.chat.di.ChatDomainModule,
        
        // Data модуль
        // com.messenger.data.di.DataModule,
        
        // Feature модули (если нужны)
    ]
)
interface AppComponent {
    
    /**
     * Фабрика для создания компонента.
     * Используется Hilt автоматически.
     */
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application
        ): AppComponent
    }
}

/**
 * Application класс с Hilt.
 * 
 * Для подключения своего API:
 * 1. Убедитесь, что все модули зарегистрированы в @Component
 * 2. Реализуйте TokenProvider в data модуле
 * 3. Настройте NetworkConfig с вашим baseURL
 */
@HiltAndroidApp
class MessengerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация глобальных настроек логирования
        // AppLogger.enabled = BuildConfig.DEBUG
        // AppLogger.minLevel = if (BuildConfig.DEBUG) AppLogger.LogLevel.DEBUG else AppLogger.LogLevel.ERROR
    }
}

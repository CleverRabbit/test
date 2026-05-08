package com.matrix.tapikapp.App

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Основной класс приложения TapikApp.
 * 
 * Инициализирует Hilt для внедрения зависимостей и настраивает логирование.
 * 
 * @author TapikApp Team
 * @since 1.0.0
 */
@HiltAndroidApp
class TapikApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Инициализация логирования только в debug режиме
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("TapikApplication: приложение запущено в режиме отладки")
        }
        
        Timber.d("TapikApplication: onCreate выполнен")
    }
}

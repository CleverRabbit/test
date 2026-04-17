package com.messenger.core.network.config

/**
 * Конфигурация сетевого клиента.
 * Содержит базовые параметры для подключения к REST API бэкенду.
 *
 * Для подключения своего API измените значения в [ApiConfig] или передайте свои через BuildConfig.
 */
object NetworkConfig {
    
    /**
     * Базовый URL API сервера.
     * ЗАМЕНИТЕ на адрес вашего бэкенда.
     */
    const val BASE_URL = "https://api.your-messenger.com/"
    
    /**
     * Таймаут подключения в миллисекундах.
     */
    const val CONNECT_TIMEOUT_MS = 30_000L
    
    /**
     * Таймаут чтения в миллисекундах.
     */
    const val READ_TIMEOUT_MS = 60_000L
    
    /**
     * Таймаут записи в миллисекундах.
     */
    const val WRITE_TIMEOUT_MS = 60_000L
    
    /**
     * Максимальное количество повторных попыток при ошибке сети.
     */
    const val MAX_RETRY_COUNT = 3
    
    /**
     * Задержка между попытками (экспоненциальная).
     */
    const val INITIAL_RETRY_DELAY_MS = 1_000L
    
    /**
     * Максимальная задержка между попытками.
     */
    const val MAX_RETRY_DELAY_MS = 30_000L
    
    /**
     * Размер чанка для загрузки файлов (в байтах).
     */
    const val CHUNK_SIZE_BYTES = 1024 * 1024 // 1 MB
    
    /**
     * Максимальный размер файла для загрузки (в байтах).
     */
    const val MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024 // 100 MB
}

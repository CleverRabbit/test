package com.messenger.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.messenger.core.common.logger.logD
import com.messenger.core.common.logger.logE
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Менеджер биометрической аутентификации.
 * Подготовлен для интеграции с E2EE шифрованием через CryptoObject.
 */
class BiometricAuthManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_NAME = "messenger_biometric_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    /**
     * Проверка доступности биометрии.
     * @return BiometricStatus с информацией о доступности
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityUpdateRequired
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.Unknown
            else -> BiometricStatus.Unknown
        }
    }

    /**
     * Показ биометрического диалога.
     * Возвращает результат аутентификации.
     * 
     * @param activity FragmentActivity для показа диалога
     * @param title заголовок диалога
     * @param subtitle подзаголовок
     * @param negativeText текст кнопки отмены
     * @param useCrypto использовать ли шифрование (для E2EE)
     * @return BiometricResult с результатом аутентификации
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Биометрическая аутентификация",
        subtitle: String = "Подтвердите личность для доступа",
        negativeText: String = "Отмена",
        useCrypto: Boolean = false
    ): BiometricResult = suspendCoroutine { continuation ->
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                logD("Biometric", "Аутентификация успешна")
                
                if (useCrypto) {
                    val cryptoObject = result.cryptoObject
                    if (cryptoObject != null) {
                        continuation.resume(BiometricResult.Success(cryptoObject))
                    } else {
                        continuation.resume(BiometricResult.Success(null))
                    }
                } else {
                    continuation.resume(BiometricResult.Success(null))
                }
            }

            override fun onAuthenticationFailed() {
                logD("Biometric", "Аутентификация не удалась (неверный отпечаток)")
                // Не завершаем корутину, позволяем пользователю повторить попытку
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                logE("Biometric", "Ошибка аутентификации: $errorCode - $errString")
                continuation.resume(BiometricResult.Error(errorCode, errString.toString()))
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        if (useCrypto) {
            try {
                val cipher = initCipher()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)
                biometricPrompt.authenticate(promptInfo, cryptoObject)
            } catch (e: Exception) {
                logE("Biometric", "Ошибка инициализации шифра", e)
                biometricPrompt.authenticate(promptInfo)
            }
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    /**
     * Инициализация Cipher для шифрования.
     * Используется для E2EE ключей.
     */
    @Throws(Exception::class)
    private fun initCipher(): Cipher {
        val keyGenerator = KeyGenerator.getInstance(
            "AES",
            ANDROID_KEYSTORE
        )
        
        // Генерируем ключ если его нет
        var secretKey: SecretKey? = getSecretKey()
        if (secretKey == null) {
            keyGenerator.init(
                KeyGenerator.ParameterSpec.Builder().build()
            )
            secretKey = keyGenerator.generateKey()
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        return cipher
    }

    /**
     * Получение секретного ключа из KeyStore.
     */
    private fun getSecretKey(): SecretKey? {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_NAME, null) as? SecretKey
    }

    /**
     * Шифрование данных с использованием биометрического ключа.
     * Для использования требуется успешная аутентификация с CryptoObject.
     */
    fun encryptData(data: ByteArray, cipher: Cipher): EncryptedData {
        val encryptedBytes = cipher.doFinal(data)
        val iv = cipher.iv
        
        return EncryptedData(
            encrypted = encryptedBytes,
            iv = iv,
            transformation = TRANSFORMATION
        )
    }

    /**
     * Расшифровка данных.
     */
    fun decryptData(encryptedData: EncryptedData, cipher: Cipher): ByteArray {
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(encryptedData.encrypted)
    }
}

/**
 * Статус доступности биометрии.
 */
sealed class BiometricStatus {
    /** Биометрия доступна */
    object Available : BiometricStatus()
    
    /** Нет биометрического оборудования */
    object NoHardware : BiometricStatus()
    
    /** Оборудование недоступно */
    object HardwareUnavailable : BiometricStatus()
    
    /** Пользователь не зарегистрировал биометрию */
    object NotEnrolled : BiometricStatus()
    
    /** Требуется обновление безопасности */
    object SecurityUpdateRequired : BiometricStatus()
    
    /** Неизвестный статус */
    object Unknown : BiometricStatus()
    
    val isAvailable: Boolean
        get() = this is Available
}

/**
 * Результат биометрической аутентификации.
 */
sealed class BiometricResult {
    /** Успешная аутентификация */
    data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : BiometricResult()
    
    /** Ошибка аутентификации */
    data class Error(val code: Int, val message: String) : BiometricResult()
    
    val isSuccess: Boolean
        get() = this is Success
}

/**
 * Зашифрованные данные.
 */
data class EncryptedData(
    val encrypted: ByteArray,
    val iv: ByteArray,
    val transformation: String
)

/**
 * Extension функция для создания менеджера.
 */
fun Context.createBiometricAuthManager(): BiometricAuthManager = BiometricAuthManager(this)

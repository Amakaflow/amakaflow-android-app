package com.amakaflow.companion.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data like JWT tokens
 * Uses EncryptedSharedPreferences for secure storage
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Lazy initialization to avoid blocking the main thread during app startup
    // EncryptedSharedPreferences requires Android Keystore access which can take 2+ seconds on cold starts
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "amakaflow_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_PROFILE = "user_profile"
        private const val KEY_DEVICE_ID = "device_id"
    }

    fun saveToken(token: String) {
        securePrefs.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return securePrefs.getString(KEY_JWT_TOKEN, null)
    }

    fun clearToken() {
        securePrefs.edit().remove(KEY_JWT_TOKEN).apply()
    }

    fun saveUserProfile(profileJson: String) {
        securePrefs.edit().putString(KEY_USER_PROFILE, profileJson).apply()
    }

    fun getUserProfile(): String? {
        return securePrefs.getString(KEY_USER_PROFILE, null)
    }

    fun clearUserProfile() {
        securePrefs.edit().remove(KEY_USER_PROFILE).apply()
    }

    fun saveDeviceId(deviceId: String) {
        securePrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceId(): String? {
        return securePrefs.getString(KEY_DEVICE_ID, null)
    }

    fun clearAll() {
        securePrefs.edit().clear().apply()
    }
}

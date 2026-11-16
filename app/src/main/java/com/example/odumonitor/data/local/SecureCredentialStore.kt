package com.example.odumonitor.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Router credentials in their own file, encrypted with an AES-256-GCM key that never
 * leaves the Android keystore.
 *
 * Only the password is a secret, but the host, username and login switch travel with it
 * so the whole record is consistent. Access is cached in memory, because a three-second
 * poll should not run keystore cryptography on every read.
 *
 * If the keystore is unusable (corrupt key, locked storage), this fails closed: the
 * value kept in memory for the session still works, but nothing new is written to disk.
 * A plaintext fallback would quietly undo the point of the class.
 */
class SecureCredentialStore(context: Context) {

    private val appContext = context.applicationContext
    private val legacyPrefs: SharedPreferences =
        appContext.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
    private val securePrefs: SharedPreferences =
        appContext.getSharedPreferences(SECURE_PREF_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cached: RouterCredentials? = null

    @Synchronized
    fun load(): RouterCredentials {
        cached?.let { return it }

        migratePlaintextIfPresent()
        val cipher = cipher()
        val credentials = RouterCredentials(
            host = readSecret(KEY_HOST, cipher) ?: DEFAULT_HOST,
            username = readSecret(KEY_USERNAME, cipher) ?: DEFAULT_USERNAME,
            password = readSecret(KEY_PASSWORD, cipher) ?: "",
            isLoginEnabled = readFlag(KEY_LOGIN_ENABLED) ?: true
        )
        cached = credentials
        return credentials
    }

    @Synchronized
    fun save(credentials: RouterCredentials) {
        cached = credentials

        val cipher = cipher() ?: return
        securePrefs.edit().apply {
            putSecret(KEY_HOST, credentials.host.trim(), cipher)
            putSecret(KEY_USERNAME, credentials.username.trim(), cipher)
            putSecret(KEY_PASSWORD, credentials.password, cipher)
            putBoolean(KEY_LOGIN_ENABLED, credentials.isLoginEnabled)
            apply()
        }
        // A write always wins over whatever the old plaintext file held, so clear it
        // here as well. Otherwise a stale legacy password would be migrated back over
        // this newer value on the next read.
        clearLegacyCredentials()
    }

    /**
     * Builds before the encrypted store existed kept the password in
     * `widget_preferences`. Move those values once, then delete the plaintext copies.
     */
    private fun migratePlaintextIfPresent() {
        if (securePrefs.getBoolean(KEY_MIGRATION_DONE, false)) return

        val legacyHost = legacyPrefs.getString(LEGACY_KEY_HOST, null)
        val legacyUsername = legacyPrefs.getString(LEGACY_KEY_USERNAME, null)
        val legacyPassword = legacyPrefs.getString(LEGACY_KEY_PASSWORD, null)
        val legacyLoginEnabled =
            if (legacyPrefs.contains(LEGACY_KEY_LOGIN_ENABLED)) {
                legacyPrefs.getBoolean(LEGACY_KEY_LOGIN_ENABLED, true)
            } else {
                null
            }

        val hasLegacyValues = CredentialMigration.hasLegacyValues(
            host = legacyHost,
            username = legacyUsername,
            password = legacyPassword,
            loginEnabled = legacyLoginEnabled
        )

        if (!hasLegacyValues) {
            securePrefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
            return
        }

        val cipher = cipher()
        if (cipher != null) {
            securePrefs.edit().apply {
                if (legacyHost != null) putSecret(KEY_HOST, legacyHost, cipher)
                if (legacyUsername != null) putSecret(KEY_USERNAME, legacyUsername, cipher)
                if (legacyPassword != null) putSecret(KEY_PASSWORD, legacyPassword, cipher)
                if (legacyLoginEnabled != null) putBoolean(KEY_LOGIN_ENABLED, legacyLoginEnabled)
                putBoolean(KEY_MIGRATION_DONE, true)
                apply()
            }
            clearLegacyCredentials()
        }
        // Without a keystore the plaintext copy stays put: it is the only record the
        // user has left, and deleting it would lose the credential outright.
    }

    private fun clearLegacyCredentials() {
        legacyPrefs.edit().apply {
            remove(LEGACY_KEY_HOST)
            remove(LEGACY_KEY_USERNAME)
            remove(LEGACY_KEY_PASSWORD)
            remove(LEGACY_KEY_LOGIN_ENABLED)
            apply()
        }
    }

    private fun readSecret(key: String, cipher: AesGcmCipher?): String? {
        val stored = securePrefs.getString(key, null) ?: return null
        if (cipher == null) return null
        return runCatching { cipher.decrypt(stored) }.getOrElse {
            Log.w(TAG, "Stored credential could not be decrypted; ignoring it")
            null
        }
    }

    private fun readFlag(key: String): Boolean? =
        if (securePrefs.contains(key)) securePrefs.getBoolean(key, true) else null

    private fun SharedPreferences.Editor.putSecret(key: String, value: String, cipher: AesGcmCipher) {
        putString(key, cipher.encrypt(value))
    }

    private fun cipher(): AesGcmCipher? {
        val key = keystoreKey() ?: return null
        return AesGcmCipher(key)
    }

    private fun keystoreKey(): SecretKey? = runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        existing ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
            generateKey()
        }
    }.onFailure {
        Log.w(TAG, "Keystore key unavailable; credentials stay in memory only")
    }.getOrNull()

    private companion object {
        const val TAG = "SecureCredentialStore"

        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "odu_router_credentials"
        const val KEY_SIZE_BITS = 256

        const val SECURE_PREF_NAME = "secure_router_credentials"
        const val LEGACY_PREF_NAME = "widget_preferences"

        const val DEFAULT_HOST = "192.168.254.1"
        const val DEFAULT_USERNAME = "Admin"

        const val KEY_HOST = "router_host"
        const val KEY_USERNAME = "router_username"
        const val KEY_PASSWORD = "router_password"
        const val KEY_LOGIN_ENABLED = "enable_router_login"
        const val KEY_MIGRATION_DONE = "plaintext_migration_done"

        const val LEGACY_KEY_HOST = "router_host"
        const val LEGACY_KEY_USERNAME = "router_username"
        const val LEGACY_KEY_PASSWORD = "router_password"
        const val LEGACY_KEY_LOGIN_ENABLED = "enable_router_login"
    }
}

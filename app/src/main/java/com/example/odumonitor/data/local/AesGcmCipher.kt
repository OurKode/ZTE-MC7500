package com.example.odumonitor.data.local

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM with a random IV per value.
 *
 * The stored form is `base64(iv || ciphertext || tag)`. GCM authenticates, so a
 * tampered or truncated value fails to decrypt instead of returning garbage.
 *
 * Encryption lets the provider pick the IV instead of handing one in. Keystore keys
 * are generated with `setRandomizedEncryptionRequired` at its default of true, and
 * Android then rejects a caller-supplied IV with
 * `InvalidAlgorithmParameterException: Caller-provided IV not permitted`. The
 * generated IV is public data, so it is stored next to the ciphertext and replayed
 * on decrypt.
 *
 * Plain JVM crypto only (no Android types), so the format can be unit tested.
 */
internal class AesGcmCipher(private val key: SecretKey) {

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val iv = cipher.iv
        require(iv.size == IV_BYTES) { "Unexpected GCM IV length: ${iv.size}" }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    fun decrypt(payload: String): String {
        val raw = Base64.getDecoder().decode(payload)
        require(raw.size > IV_BYTES) { "Encrypted payload is too short" }
        val iv = raw.copyOfRange(0, IV_BYTES)
        val ciphertext = raw.copyOfRange(IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

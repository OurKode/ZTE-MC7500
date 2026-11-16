package com.example.odumonitor.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64
import javax.crypto.KeyGenerator

class AesGcmCipherTest {

    private val cipher = AesGcmCipher(
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    )

    @Test
    fun `round trip returns the original text`() {
        val secret = "s3cr3t-router-password"
        assertEquals(secret, cipher.decrypt(cipher.encrypt(secret)))
    }

    @Test
    fun `same plaintext encrypts to different payloads`() {
        val first = cipher.encrypt("admin")
        val second = cipher.encrypt("admin")
        assertNotEquals(first, second)
        assertEquals("admin", cipher.decrypt(first))
        assertEquals("admin", cipher.decrypt(second))
    }

    @Test
    fun `payload carries a twelve byte iv`() {
        val raw = Base64.getDecoder().decode(cipher.encrypt("x"))
        assertEquals(12 + "x".toByteArray().size + 16, raw.size)
    }

    @Test
    fun `tampered payload is rejected`() {
        val raw = Base64.getDecoder().decode(cipher.encrypt("router"))
        raw[raw.lastIndex] = (raw.last().toInt() xor 0x01).toByte()
        val tampered = Base64.getEncoder().encodeToString(raw)
        assertThrows(Exception::class.java) { cipher.decrypt(tampered) }
    }

    @Test
    fun `truncated payload is rejected`() {
        val raw = Base64.getDecoder().decode(cipher.encrypt("router"))
        val truncated = Base64.getEncoder().encodeToString(raw.copyOfRange(0, 8))
        assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(truncated) }
    }

    @Test
    fun `empty string survives the round trip`() {
        assertEquals("", cipher.decrypt(cipher.encrypt("")))
    }
}

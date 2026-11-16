package com.example.odumonitor.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialMigrationTest {

    @Test
    fun `empty legacy file needs no migration`() {
        val migrate = CredentialMigration.hasLegacyValues(
            host = null,
            username = null,
            password = null,
            loginEnabled = null
        )
        assertFalse(migrate)
    }

    @Test
    fun `legacy password alone is enough`() {
        val migrate = CredentialMigration.hasLegacyValues(
            host = null,
            username = null,
            password = "secret",
            loginEnabled = null
        )
        assertTrue(migrate)
    }

    @Test
    fun `legacy host without a password is still migrated`() {
        val migrate = CredentialMigration.hasLegacyValues(
            host = "192.168.1.1",
            username = null,
            password = null,
            loginEnabled = null
        )
        assertTrue(migrate)
    }

    @Test
    fun `legacy login switch alone is still migrated`() {
        val migrate = CredentialMigration.hasLegacyValues(
            host = null,
            username = null,
            password = null,
            loginEnabled = false
        )
        assertTrue(migrate)
    }
}

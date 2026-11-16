package com.example.odumonitor.data.local

/**
 * Pure decision logic for the one-time move from plaintext preferences to the
 * encrypted credential file. Kept free of Android types so the rules can be tested on
 * the JVM; [SecureCredentialStore] supplies the values.
 */
internal object CredentialMigration {

    /**
     * True when the legacy preference file still holds any credential value.
     *
     * The host and username count on their own: a user who saved a host but never a
     * password would otherwise lose that host when the encrypted store takes over.
     */
    fun hasLegacyValues(
        host: String?,
        username: String?,
        password: String?,
        loginEnabled: Boolean?
    ): Boolean = host != null || username != null || password != null || loginEnabled != null
}

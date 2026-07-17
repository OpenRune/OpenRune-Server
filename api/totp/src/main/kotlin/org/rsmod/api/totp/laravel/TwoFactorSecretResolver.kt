package org.rsmod.api.totp.laravel

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class TwoFactorSecretResolver @Inject constructor(
    settingsLoader: LaravelSettingsLoader,
) {
    private val logger = InlineLogger()

    private val encrypter: LaravelEncrypter? =
        try {
            val appKey = settingsLoader.loadAppKey()
            if (appKey == null) {
                null
            } else {
                LaravelEncrypter(appKey)
            }
        } catch (e: Exception) {
            logger.error(e) { "Invalid Laravel APP_KEY; encrypted two-factor secrets cannot be decoded" }
            null
        }

    /**
     * Returns the base32 TOTP secret for verification. Laravel Fortify stores an encrypted,
     * PHP-serialized secret in `accounts.two_factor_secret`; legacy rows may be plaintext base32.
     */
    public fun resolveStoredSecret(stored: String): CharArray? {
        val trimmed = stored.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        return try {
            val plaintext =
                if (LaravelEncrypter.appearsEncrypted(trimmed)) {
                    encrypter?.decryptString(trimmed)
                        ?: run {
                            logger.error {
                                "Encrypted two-factor secret but Laravel APP_KEY is not configured " +
                                    "(copy api/totp/src/main/resources/laravel-settings.example.yml " +
                                    "to laravel-settings.yml)"
                            }
                            return null
                        }
                } else {
                    trimmed
                }
            plaintext.toCharArray()
        } catch (e: Exception) {
            logger.error(e) { "Failed to decode stored two-factor secret" }
            null
        }
    }
}

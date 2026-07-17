package org.rsmod.api.totp.laravel

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

public class LaravelEncrypter(
    appKey: String,
) {
    private val key: ByteArray = decodeAppKey(appKey)

    public fun decryptString(payload: String): String {
        val json = decodePayload(payload)
        val mac = hmacSha256(json.iv + json.value, key)
        if (!mac.equals(json.mac, ignoreCase = true)) {
            throw IllegalArgumentException("Invalid Laravel encryption MAC")
        }
        val iv = Base64.getDecoder().decode(json.iv)
        val cipher = Cipher.getInstance("$JCE_CIPHER_PREFIX/$JCE_CIPHER_SUFFIX/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, JCE_CIPHER_PREFIX), IvParameterSpec(iv))
        val decrypted = cipher.doFinal(Base64.getDecoder().decode(json.value))
        val serialized = decrypted.decodeToString()
        return PhpUnserialize.readString(serialized)
    }

    public companion object {
        private const val JCE_CIPHER_PREFIX = "AES"
        private const val JCE_CIPHER_SUFFIX = "CBC"

        public fun appearsEncrypted(value: String): Boolean {
            val decoded =
                try {
                    Base64.getDecoder().decode(value)
                } catch (_: IllegalArgumentException) {
                    return false
                }
            return try {
                val tree = ObjectMapper().readTree(decoded)
                tree.hasNonNull("iv") && tree.hasNonNull("value") && tree.hasNonNull("mac")
            } catch (_: Exception) {
                false
            }
        }

        private fun decodeAppKey(appKey: String): ByteArray {
            val trimmed = appKey.trim()
            val encoded =
                if (trimmed.startsWith("base64:")) {
                    trimmed.removePrefix("base64:")
                } else {
                    trimmed
                }
            val key = Base64.getDecoder().decode(encoded)
            require(key.size == 32) { "Laravel APP_KEY must decode to 32 bytes for AES-256-CBC" }
            return key
        }

        private fun decodePayload(payload: String): PayloadJson {
            val decoded = Base64.getDecoder().decode(payload)
            val tree = ObjectMapper().readTree(decoded)
            return PayloadJson(
                iv = tree.get("iv").asText(),
                value = tree.get("value").asText(),
                mac = tree.get("mac").asText(),
            )
        }

        private fun hmacSha256(data: String, key: ByteArray): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            val digest = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
        }
    }

    private data class PayloadJson(
        val iv: String,
        val value: String,
        val mac: String,
    )
}

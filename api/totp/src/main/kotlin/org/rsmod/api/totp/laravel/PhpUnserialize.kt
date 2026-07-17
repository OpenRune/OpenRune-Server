package org.rsmod.api.totp.laravel

/**
 * Minimal PHP `unserialize` support for Laravel-encrypted string payloads (`s:len:"value";`).
 */
internal object PhpUnserialize {
    fun readString(serialized: String): String {
        if (!serialized.startsWith("s:")) {
            throw IllegalArgumentException("Expected PHP serialized string")
        }
        val firstQuote = serialized.indexOf('"', 2)
        if (firstQuote < 0) {
            throw IllegalArgumentException("Malformed PHP serialized string")
        }
        val lengthEnd = serialized.indexOf(':', 2)
        val byteLength = serialized.substring(2, lengthEnd).toInt()
        var index = firstQuote + 1
        val output = StringBuilder(byteLength)
        var consumed = 0
        while (consumed < byteLength && index < serialized.length) {
            val char = serialized[index++]
            if (char == '\\' && index < serialized.length) {
                output.append(serialized[index++])
            } else {
                output.append(char)
            }
            consumed++
        }
        return output.toString()
    }
}

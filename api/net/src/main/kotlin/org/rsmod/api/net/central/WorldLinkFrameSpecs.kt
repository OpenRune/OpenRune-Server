package org.rsmod.api.net.central

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/** Length-prefixed binary world-link protocol between game server and OpenRune Central. */
public object WorldLinkFrameSpecs {
    public const val MAX_FRAMED_BODY: Int = 16 * 1024

    public const val MAGIC: Int = 0x4F523231
    public const val TOKEN_BYTES: Int = 32
    public const val CLIENT_PROTOCOL_VERSION: Int = 5

    public const val WORLD_KEY_MAX_BYTES: Int = 4096

    public const val LOGIN_FAIL_SCRIPT_LINE_MAX_UTF8_BYTES: Int = 512

    private const val LOGIN_FAIL_MAX_BODY: Int =
        4 + 3 * (2 + LOGIN_FAIL_SCRIPT_LINE_MAX_UTF8_BYTES)
    private const val LOGIN_USERNAME_MAX_UTF8: Int = 64 * 4
    private const val LOGIN_PASSWORD_MAX_UTF8: Int = 256 * 4
    private const val TOKEN_BODY_BYTES: Int = 2 + TOKEN_BYTES

    public const val OP_WORLD_HELLO: Int = 0x01
    public const val OP_HELLO_ACK: Int = 0x02
    public const val OP_HELLO_REJECT: Int = 0x03
    public const val OP_LOGIN: Int = 0x10
    public const val OP_LOGIN_OK: Int = 0x11
    public const val OP_LOGIN_FAIL: Int = 0x12
    public const val OP_PUSH_SUBSCRIBE: Int = 0x13
    public const val OP_PUSH_SUBSCRIBE_ACK: Int = 0x14
    public const val OP_HEARTBEAT: Int = 0x30
    public const val OP_LOGOUT: Int = 0x40
    public const val OP_LOGOUT_ACK: Int = 0x41

    public const val OP_SERVER_REVOKE_LOGIN: Int = 0x50

    public const val OP_SERVER_MUTE_UPDATE: Int = 0x51

    public const val OP_SERVER_KICK: Int = 0x52

    public const val OP_SERVER_REBOOT: Int = 0x54

    public const val OP_SERVER_BROADCAST: Int = 0x55

    private const val LOGIN_OK_RIGHTS_MAX_BYTES: Int = 4096
    private const val LOGIN_OK_MAX_BODY: Int = 2 + TOKEN_BYTES + 8 + 2 + LOGIN_OK_RIGHTS_MAX_BYTES
    private const val LOGIN_OK_MIN_BODY: Int = 2 + TOKEN_BYTES + 8

    private const val WORLD_OPS_UTF8_MAX: Int = 2048

    private const val SERVER_REBOOT_MIN_BODY: Int = 1 + 4 + 8 + 2 + 0

    /** Human-readable explanation for a non-null result from [validateGameToCentralBody] or [validateCentralToGameFrame]. */
    public fun describeValidationFailure(reason: String): String =
        when (reason) {
            "empty" -> "frame was empty (expected at least an opcode byte)"
            "hello_ack_size" -> "HELLO_ACK must have no body after the opcode"
            "hello_reject_size" -> "HELLO_REJECT must have exactly one reason byte after the opcode"
            "login_fail_short" -> "LOGIN_FAIL body too short (need at least the 4-byte failure code)"
            "login_fail_long" -> "LOGIN_FAIL body exceeds the maximum allowed size"
            "login_fail_script" -> "LOGIN_FAIL optional script trailer is truncated or malformed"
            "login_fail_script_line" -> "LOGIN_FAIL script line exceeds max UTF-8 length"
            "login_fail_trailing" -> "LOGIN_FAIL has unexpected trailing bytes after the script lines"
            "login_ok_size" -> "LOGIN_OK body length is outside the allowed range for token + account id + rights"
            "logout_ack_size" -> "LOGOUT_ACK must have no body after the opcode"
            "push_subscribe_ack_size" -> "PUSH_SUBSCRIBE_ACK must have no body after the opcode"
            "server_revoke_login_size" -> "SERVER_REVOKE_LOGIN body must be exactly 12 bytes (account id + character id)"
            "server_mute_update_size" -> "SERVER_MUTE_UPDATE body must be exactly 20 bytes"
            "server_kick_size" -> "SERVER_KICK body must be exactly 12 bytes (account id + character id)"
            "server_reboot_short" -> "SERVER_REBOOT body is shorter than the minimum fixed fields + message length"
            "server_reboot_long" -> "SERVER_REBOOT body exceeds the maximum allowed size"
            "server_reboot_msg_len" -> "SERVER_REBOOT message UTF-8 block length is invalid"
            "server_reboot_msg_mismatch" -> "SERVER_REBOOT declared message length does not match frame size"
            "server_broadcast_size" -> "SERVER_BROADCAST body length is outside the allowed range"
            "server_broadcast_truncated" -> "SERVER_BROADCAST is truncated before a length-prefixed string"
            "server_broadcast_chunk_len" -> "SERVER_BROADCAST string length exceeds maximum UTF-8 size"
            "server_broadcast_chunk_data" -> "SERVER_BROADCAST string data is truncated"
            "server_broadcast_trailing" -> "SERVER_BROADCAST has unexpected trailing bytes"
            "unexpected_opcode" -> "opcode is not defined for this direction in the protocol"
            "too_short" -> "frame body is shorter than the minimum for this opcode"
            "too_long" -> "frame body is longer than the maximum for this opcode"
            "push_subscribe_size" -> "PUSH_SUBSCRIBE must have no body after the opcode"
            "bad_token_frame" -> "frame must carry token as u16 length + exactly $TOKEN_BYTES bytes"
            "unknown_opcode" -> "opcode is not valid for game-to-Central frames"
            else -> "validation code: $reason"
        }

    public fun validateGameToCentralBody(
        opcode: Int,
        bodyLen: Int,
    ): String? =
        when (opcode) {
            OP_WORLD_HELLO -> {
                val min = 4 + 2 + 4 + 2 + 0
                val max = 4 + 2 + 4 + 2 + WORLD_KEY_MAX_BYTES
                if (bodyLen < min) "too_short" else if (bodyLen > max) "too_long" else null
            }
            OP_LOGIN -> {
                val min = 2 + 0 + 2 + 0
                val max = 2 + LOGIN_USERNAME_MAX_UTF8 + 2 + LOGIN_PASSWORD_MAX_UTF8 + 4
                if (bodyLen < min) "too_short" else if (bodyLen > max) "too_long" else null
            }
            OP_PUSH_SUBSCRIBE ->
                if (bodyLen != 0) {
                    "push_subscribe_size"
                } else {
                    null
                }
            OP_HEARTBEAT, OP_LOGOUT -> {
                if (bodyLen != TOKEN_BODY_BYTES) "bad_token_frame" else null
            }
            else -> "unknown_opcode"
        }

    /** Central → game full frame (includes opcode as [frame][0]). */
    public fun validateCentralToGameFrame(frame: ByteArray): String? {
        if (frame.isEmpty()) {
            return "empty"
        }
        val op = frame[0].toInt() and 0xFF
        val bodyLen = frame.size - 1
        return when (op) {
            OP_HELLO_ACK ->
                if (bodyLen != 0) {
                    "hello_ack_size"
                } else {
                    null
                }
            OP_HELLO_REJECT ->
                if (bodyLen != 1) {
                    "hello_reject_size"
                } else {
                    null
                }
            OP_LOGIN_FAIL -> validateLoginFailFrame(frame)
            OP_LOGIN_OK ->
                if (bodyLen < LOGIN_OK_MIN_BODY || bodyLen > LOGIN_OK_MAX_BODY) {
                    "login_ok_size"
                } else {
                    null
                }
            OP_LOGOUT_ACK ->
                if (bodyLen != 0) {
                    "logout_ack_size"
                } else {
                    null
                }
            OP_PUSH_SUBSCRIBE_ACK ->
                if (bodyLen != 0) {
                    "push_subscribe_ack_size"
                } else {
                    null
                }
            OP_SERVER_REVOKE_LOGIN ->
                if (bodyLen != 8 + 4) {
                    "server_revoke_login_size"
                } else {
                    null
                }
            OP_SERVER_MUTE_UPDATE ->
                if (bodyLen != 8 + 4 + 8) {
                    "server_mute_update_size"
                } else {
                    null
                }
            OP_SERVER_KICK ->
                if (bodyLen != 8 + 4) {
                    "server_kick_size"
                } else {
                    null
                }
            OP_SERVER_REBOOT -> validateServerRebootFrame(frame)
            OP_SERVER_BROADCAST -> validateServerBroadcastFrame(frame)
            else -> "unexpected_opcode"
        }
    }

    private fun validateLoginFailFrame(frame: ByteArray): String? {
        val bodyLen = frame.size - 1
        if (bodyLen < 4) {
            return "login_fail_short"
        }
        if (bodyLen > LOGIN_FAIL_MAX_BODY) {
            return "login_fail_long"
        }
        if (bodyLen == 4) {
            return null
        }
        val buf = ByteBuffer.wrap(frame, 5, bodyLen - 4)
        repeat(3) {
            if (buf.remaining() < 2) {
                return "login_fail_script"
            }
            val chunk = buf.short.toInt() and 0xFFFF
            if (chunk > LOGIN_FAIL_SCRIPT_LINE_MAX_UTF8_BYTES) {
                return "login_fail_script_line"
            }
            if (buf.remaining() < chunk) {
                return "login_fail_script"
            }
            buf.position(buf.position() + chunk)
        }
        return if (buf.remaining() != 0) "login_fail_trailing" else null
    }

    private fun validateServerRebootFrame(frame: ByteArray): String? {
        val bodyLen = frame.size - 1
        if (bodyLen < SERVER_REBOOT_MIN_BODY) {
            return "server_reboot_short"
        }
        if (bodyLen > SERVER_REBOOT_MIN_BODY + WORLD_OPS_UTF8_MAX) {
            return "server_reboot_long"
        }
        val buf = ByteBuffer.wrap(frame, 1, bodyLen)
        buf.get()
        buf.int
        buf.long
        val msgLen = buf.short.toInt() and 0xFFFF
        if (msgLen > WORLD_OPS_UTF8_MAX) {
            return "server_reboot_msg_len"
        }
        if (15 + msgLen != bodyLen) {
            return "server_reboot_msg_mismatch"
        }
        return null
    }

    private fun validateServerBroadcastFrame(frame: ByteArray): String? {
        val bodyLen = frame.size - 1
        val min = 4 + 2 + 0 + 2 + 0 + 2 + 0
        val max = 4 + (2 + WORLD_OPS_UTF8_MAX) * 3
        if (bodyLen < min || bodyLen > max) {
            return "server_broadcast_size"
        }
        val buf = ByteBuffer.wrap(frame, 1, bodyLen)
        buf.int
        repeat(3) {
            if (buf.remaining() < 2) {
                return "server_broadcast_truncated"
            }
            val chunk = buf.short.toInt() and 0xFFFF
            if (chunk > WORLD_OPS_UTF8_MAX) {
                return "server_broadcast_chunk_len"
            }
            if (buf.remaining() < chunk) {
                return "server_broadcast_chunk_data"
            }
            buf.position(buf.position() + chunk)
        }
        return if (buf.remaining() != 0) "server_broadcast_trailing" else null
    }

    // --- Inbound push payloads (call only when [validateCentralToGameFrame] returned null) ---

    public data class ServerRevokeLoginPayload(
        val accountId: Long,
        val characterId: Int,
    )

    public data class ServerKickPayload(
        val accountId: Long,
        val characterId: Int,
    )

    public data class ServerMuteUpdatePayload(
        val accountId: Long,
        val characterId: Int,
        val mutedUntilEpochMillis: Long,
    )

    public data class ServerRebootPayload(
        val clear: Boolean,
        val worldScope: Int,
        val rebootAtMs: Long,
        val message: String,
    )

    public data class ServerBroadcastPayload(
        val worldScope: Int,
        val message: String,
        val url: String,
        val icon: String,
    )

    public fun decodeServerRevokeLogin(frame: ByteArray): ServerRevokeLoginPayload {
        val buf = ByteBuffer.wrap(frame, 1, frame.size - 1)
        return ServerRevokeLoginPayload(buf.long, buf.int)
    }

    public fun decodeServerKick(frame: ByteArray): ServerKickPayload {
        val buf = ByteBuffer.wrap(frame, 1, frame.size - 1)
        return ServerKickPayload(buf.long, buf.int)
    }

    public fun decodeServerMuteUpdate(frame: ByteArray): ServerMuteUpdatePayload {
        val buf = ByteBuffer.wrap(frame, 1, frame.size - 1)
        return ServerMuteUpdatePayload(buf.long, buf.int, buf.long)
    }

    public fun decodeServerReboot(frame: ByteArray): ServerRebootPayload {
        val buf = ByteBuffer.wrap(frame, 1, frame.size - 1)
        val kind = buf.get().toInt() and 0xFF
        val worldScope = buf.int
        val rebootAtMs = buf.long
        val message = readUtf16LengthPrefixed(buf)
        return ServerRebootPayload(clear = kind == 1, worldScope, rebootAtMs, message)
    }

    public fun decodeServerBroadcast(frame: ByteArray): ServerBroadcastPayload {
        val buf = ByteBuffer.wrap(frame, 1, frame.size - 1)
        val worldScope = buf.int
        val message = readUtf16LengthPrefixed(buf)
        val url = readUtf16LengthPrefixed(buf)
        val icon = readUtf16LengthPrefixed(buf)
        return ServerBroadcastPayload(worldScope, message, url, icon)
    }

    private fun readUtf16LengthPrefixed(buf: ByteBuffer): String {
        val len = buf.short.toInt() and 0xFFFF
        val bytes = ByteArray(len)
        buf.get(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}

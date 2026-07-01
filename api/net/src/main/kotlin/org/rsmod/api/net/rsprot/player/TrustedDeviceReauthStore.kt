package org.rsmod.api.net.rsprot.player

import java.util.concurrent.ConcurrentHashMap

internal object TrustedDeviceReauthStore {
    private val pendingDeviceIds = ConcurrentHashMap<String, Int>()

    fun remember(accountName: String, deviceId: Int) {
        pendingDeviceIds[accountName.lowercase()] = deviceId
    }

    fun take(accountName: String): Int? = pendingDeviceIds.remove(accountName.lowercase())
}

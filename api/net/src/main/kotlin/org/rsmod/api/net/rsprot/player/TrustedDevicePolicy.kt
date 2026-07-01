package org.rsmod.api.net.rsprot.player

import dev.or2.central.account.TrustedDeviceData
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal object TrustedDevicePolicy {
    const val DAYS_BETWEEN_2FA_VERIFICATION: Long = 30

    fun requiresTwoFactor(
        trustedDevices: List<TrustedDeviceData>,
        auth: TrustedDeviceAuth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean =
        when (auth) {
            TrustedDeviceAuth.Unknown -> true
            is TrustedDeviceAuth.Known -> {
                val device = trustedDevices.firstOrNull { it.deviceId == auth.deviceId } ?: return true
                daysSinceVerification(device, zone) >= DAYS_BETWEEN_2FA_VERIFICATION
            }
        }

    fun rememberReauthIfNeeded(
        accountName: String,
        trustedDevices: List<TrustedDeviceData>,
        auth: TrustedDeviceAuth,
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        if (auth !is TrustedDeviceAuth.Known) {
            return
        }
        val device = trustedDevices.firstOrNull { it.deviceId == auth.deviceId } ?: return
        if (daysSinceVerification(device, zone) >= DAYS_BETWEEN_2FA_VERIFICATION) {
            TrustedDeviceReauthStore.remember(accountName, auth.deviceId)
        }
    }

    private fun daysSinceVerification(
        device: TrustedDeviceData,
        zone: ZoneId,
    ): Long =
        ChronoUnit.DAYS.between(
            device.verifiedAt.atZone(zone).toLocalDate(),
            LocalDate.now(zone),
        )
}

internal sealed class TrustedDeviceAuth {
    data object Unknown : TrustedDeviceAuth()

    data class Known(val deviceId: Int) : TrustedDeviceAuth()
}

internal fun upsertTrustedDevice(
    devices: MutableList<TrustedDeviceData>,
    deviceId: Int,
    verifiedAt: java.time.LocalDateTime,
): MutableList<TrustedDeviceData> {
    val index = devices.indexOfFirst { it.deviceId == deviceId }
    val entry = TrustedDeviceData(deviceId, verifiedAt)
    if (index >= 0) {
        devices[index] = entry
    } else {
        devices.add(entry)
    }
    return devices
}

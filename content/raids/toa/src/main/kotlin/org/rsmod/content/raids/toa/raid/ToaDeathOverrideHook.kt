package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathOverrideHook
import org.rsmod.api.player.hasProtectItemPrayer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRetrieval.protectItemAtDeath

class ToaDeathOverrideHook @Inject constructor() : PlayerDeathOverrideHook {
    override suspend fun ProtectedAccess.override(): Boolean {
        val raid = player.currentRaid ?: return false
        if (!raid.startDying(player)) return true
        player.protectItemAtDeath = player.hasProtectItemPrayer()
        raidDeath()
        return true
    }
}

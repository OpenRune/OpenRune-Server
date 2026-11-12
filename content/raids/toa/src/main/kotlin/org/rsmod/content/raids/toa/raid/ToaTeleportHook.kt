package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Player

class ToaTeleportHook @Inject constructor() : PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? {
        val raid = player.currentRaid ?: return null
        if (!raid.isGhost(player)) return null
        return "A mysterious force prevents you from doing that."
    }
}

package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Player

/**
 * Offline_Scape TOARaidArea.canTeleport: a ghost can't teleport away. Anyone else may; teleporting
 * out leaves the raid (ToaRaidScript's area exit).
 *
 * Only spells, tablets and the like reach this hook. The raid's own moves use
 * [TeleportType.Exempt], which PlayerTeleportValidator lets through without asking any hook.
 */
class ToaTeleportHook @Inject constructor() : PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? {
        val raid = player.currentRaid ?: return null
        return if (raid.isGhost(player)) "A mysterious force prevents you from doing that." else null
    }
}

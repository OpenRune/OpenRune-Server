package org.rsmod.content.minigames.gauntlet

import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Player

internal class GauntletTeleportHook : PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (player.inGauntlet && type != TeleportType.Exempt) "You cannot teleport out of the Gauntlet. Use the exit." else null
}

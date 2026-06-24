package org.rsmod.api.player.hook

import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Player

public fun interface PlayerTeleportValidateHook {
    /**
     * @return A denial message if the teleport should be blocked, or `null` if allowed.
     */
    public fun validate(
        player: Player,
        type: TeleportType,
        areaChecker: AreaChecker,
    ): String?
}

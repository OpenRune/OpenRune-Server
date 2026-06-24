package org.rsmod.api.player.hook

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Player

public class PlayerTeleportValidator
@Inject
constructor(private val hooks: Set<@JvmSuppressWildcards PlayerTeleportValidateHook>) {
    public fun validate(
        player: Player,
        type: TeleportType,
        areaChecker: AreaChecker,
    ): String? {
        if (type == TeleportType.Exempt) {
            return null
        }
        for (hook in hooks) {
            val denial = hook.validate(player, type, areaChecker)
            if (denial != null) {
                return denial
            }
        }
        return null
    }
}

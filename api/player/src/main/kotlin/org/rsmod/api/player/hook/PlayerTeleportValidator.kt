package org.rsmod.api.player.hook

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.PlayerTeleportValidateEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class PlayerTeleportValidator
@Inject
constructor(
    private val hooks: Set<@JvmSuppressWildcards PlayerTeleportValidateHook>,
    private val eventBus: EventBus,
) {
    public fun validate(
        player: Player,
        dest: CoordGrid,
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
        val event = PlayerTeleportValidateEvent(player, dest, type)
        eventBus.publish(event)
        return event.refusal
    }
}

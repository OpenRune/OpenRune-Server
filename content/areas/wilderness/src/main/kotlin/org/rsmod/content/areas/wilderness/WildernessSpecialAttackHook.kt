package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.death.PvPSpecialAttackHook
import org.rsmod.content.areas.wilderness.WildernessTeleportHook.Companion.LAST_PVP_OFFENSIVE_SPEC_TICK_ATTR
import org.rsmod.game.entity.Player

public class WildernessSpecialAttackHook @Inject constructor() : PvPSpecialAttackHook {
    override fun onPlayerSpecialAttack(attacker: Player, target: Player) {
        attacker.attr[LAST_PVP_OFFENSIVE_SPEC_TICK_ATTR] = attacker.currentMapClock
    }
}

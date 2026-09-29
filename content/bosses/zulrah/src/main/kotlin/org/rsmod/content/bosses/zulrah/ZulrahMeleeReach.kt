package org.rsmod.content.bosses.zulrah

import dev.openrune.util.WeaponCategory
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.NpcMeleeRangeHook
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

class ZulrahMeleeReach @Inject constructor() : NpcMeleeRangeHook {
    override fun range(player: Player, target: Npc, weaponRange: Int): Int? {
        if (ZulrahCombatScript.BOSS_TYPES.none { target.visType.isType(it) }) return null
        val weapon = getOrNull(player.righthand) ?: return null
        val extendedReach =
            weaponRange > 1 || weapon.weaponCategory == WeaponCategory.Polearm ||
                weapon.weaponCategory == WeaponCategory.Scythe
        return if (extendedReach) maxOf(weaponRange, SHORE_REACH) else null
    }

    companion object {
        private const val SHORE_REACH = 3
    }
}

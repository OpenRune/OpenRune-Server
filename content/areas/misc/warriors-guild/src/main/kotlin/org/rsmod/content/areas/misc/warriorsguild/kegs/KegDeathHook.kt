package org.rsmod.content.areas.misc.warriorsguild.kegs

import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.content.areas.misc.warriorsguild.kegs.KegScript.Companion.clearKegs
import org.rsmod.game.entity.Player

/** The barrels are the guild's kegs, not loot: they vanish before a death drops anything. */
internal class KegDeathHook : PlayerDeathPreDropHook {
    override fun beforeDrops(player: Player) {
        clearKegs(player)
    }
}

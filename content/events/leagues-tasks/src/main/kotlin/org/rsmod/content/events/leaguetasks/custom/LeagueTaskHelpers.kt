package org.rsmod.content.events.leaguetasks.custom

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

internal fun task(name: String): Int = "dbrow.$name".asRSCM(RSCMType.DBROW)

internal fun npcs(vararg names: String): Set<Int> =
    names.mapTo(HashSet()) { it.asRSCM(RSCMType.NPC) }

internal fun objs(vararg names: String): Set<Int> =
    names.mapTo(HashSet()) { it.asRSCM(RSCMType.OBJ) }

internal fun Player.advance(counter: String, cap: Int): Int {
    val current = vars[counter]
    if (current >= cap) {
        return current
    }
    VarPlayerIntMapSetter.set(this, counter, current + 1)
    return current + 1
}

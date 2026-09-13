package org.rsmod.api.death

import org.rsmod.api.config.constants
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public data class NpcDeathKillContext(
    public val hero: Player,
    public val npc: Npc,
    public val lootTrackerEventId: Int,
    public val dropCoords: CoordGrid = npc.coords,
    public val dropDuration: Int = hero.lootDropDuration ?: constants.lootdrop_duration,
)

/**
 * Invoked after NPC death drop handling for the killing player ([NpcDeathKillContext.hero]), whether
 * or not a remains drop was consumed (for example by the bonecrusher). Use for effects that depend
 * on the kill rather than on a specific ground drop.
 */
public fun interface NpcDeathKillHook {
    public fun onKill(context: NpcDeathKillContext)
}

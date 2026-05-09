package org.rsmod.api.death

import dev.openrune.types.ItemServerType
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public data class NpcDeathDropContext(
    public val hero: Player,
    public val dropType: ItemServerType,
    public val dropCoords: CoordGrid,
    public val duration: Int,
    public val objRepo: ObjRepository,
)

public fun interface NpcDeathDropHook {
    public fun tryConsume(context: NpcDeathDropContext): Boolean
}

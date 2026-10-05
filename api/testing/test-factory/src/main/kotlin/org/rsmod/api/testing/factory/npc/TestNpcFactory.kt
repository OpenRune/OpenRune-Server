package org.rsmod.api.testing.factory.npc

import dev.openrune.types.NpcServerType
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

public class TestNpcFactory {
    public fun create(
        type: NpcServerType,
        coords: CoordGrid = CoordGrid.ZERO,
        init: Npc.() -> Unit = {},
    ): Npc = Npc(type, coords).apply(init)
}

package org.rsmod.game.movement

import dev.openrune.util.BlockWalk
import org.rsmod.routefinder.flag.CollisionFlag

public val BlockWalk.collisionFlag: Int?
    get() =
        when (this) {
            BlockWalk.Npc -> CollisionFlag.BLOCK_NPCS
            BlockWalk.All -> CollisionFlag.BLOCK_PLAYERS or CollisionFlag.BLOCK_NPCS
            BlockWalk.None -> null
        }

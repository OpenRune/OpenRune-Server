package org.rsmod.api.death

import org.rsmod.game.entity.Player

/** Runs once before carried items are captured, including deaths that bypass item drops. */
public fun interface PlayerDeathPreDropHook {
    public fun beforeDrops(player: Player)
}

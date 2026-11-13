package org.rsmod.api.player.hook

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp

/**
 * Takes over an npc op before the npc's own handler, for systems that can target any npc (e.g.
 * clue scroll steps). A claiming hook also gives an npc without a handler an op trigger.
 */
public interface PlayerNpcOpHook {
    public fun claims(player: Player, npc: Npc, op: InteractionOp): Boolean

    public suspend fun ProtectedAccess.op(npc: Npc, op: InteractionOp)
}

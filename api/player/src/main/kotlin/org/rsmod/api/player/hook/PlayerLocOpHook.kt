package org.rsmod.api.player.hook

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.BoundLocInfo

/**
 * Takes over a loc op before the loc's own handler, for systems that can target any loc (e.g. clue
 * scroll search steps). A claiming hook also gives a loc without a handler an op trigger.
 */
public interface PlayerLocOpHook {
    public fun claims(player: Player, loc: BoundLocInfo, op: InteractionOp): Boolean

    public suspend fun ProtectedAccess.op(loc: BoundLocInfo, op: InteractionOp)
}

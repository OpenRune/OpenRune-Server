package org.rsmod.game.type

import dev.openrune.types.ObjectServerType
import org.rsmod.game.interact.InteractionOp

// hasOp
public fun ObjectServerType.hasOp(interactionOp: InteractionOp): Boolean {
    val text = actions.getOpOrNull(interactionOp.slot - 1) ?: return false
    val invalid = text.isBlank() || text.equals("hidden", ignoreCase = true)
    return !invalid
}



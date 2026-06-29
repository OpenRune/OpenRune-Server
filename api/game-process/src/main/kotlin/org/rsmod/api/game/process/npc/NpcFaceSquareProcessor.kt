package org.rsmod.api.game.process.npc

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.map.CoordGrid

public class NpcFaceSquareProcessor {
    public fun process(npc: Npc) {
        npc.processFaceSquare()
    }

    private fun Npc.processFaceSquare() {
        // While facing is locked, force the angle toward the locked square every tick - this overrides
        // movement- and combat-driven facing so the npc keeps facing its pinned direction even when
        // attacking or being attacked.
        if (isFacingLocked) {
            val angle = calculateAngle(faceLockSquare, faceLockWidth, faceLockLength)
            pendingFaceAngle = EntityFaceAngle.fromOrNull(angle)
            resetPendingFaceSquare()
            return
        }
        if (!hasMovedThisCycle && pendingFaceSquare != CoordGrid.NULL) {
            val angle = calculateAngle(pendingFaceSquare, pendingFaceWidth, pendingFaceLength)
            pendingFaceAngle = EntityFaceAngle.fromOrNull(angle)
            resetPendingFaceSquare()
        }
    }
}

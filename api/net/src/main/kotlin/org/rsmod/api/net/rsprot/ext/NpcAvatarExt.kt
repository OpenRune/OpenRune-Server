package org.rsmod.api.net.rsprot.ext

import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcAvatarExtendedInfo
import org.rsmod.game.entity.util.EntityFaceTarget

fun NpcAvatarExtendedInfo.setFaceTarget(target: EntityFaceTarget) {
    when {
        target.entitySlot == -1 -> resetFacing()
        target.isNpc -> setFaceNpc(
            target.npcSlot,
            instant = false,
            walkMode = 0,
            entityFallbackAngle = 0,
        )

        target.isPlayer ->
            setFacePlayer(target.playerSlot, instant = false, walkMode = 0, entityFallbackAngle = 0)
    }
}

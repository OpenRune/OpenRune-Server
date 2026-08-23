package org.rsmod.api.net.rsprot

import net.rsprot.protocol.game.outgoing.info.worldentityinfo.WorldEntityAvatar
import org.rsmod.game.entity.worldentity.WorldEntityInfoProtocol

class RspWorldEntityInfo(val rspAvatar: WorldEntityAvatar) : WorldEntityInfoProtocol {
    override fun updateCoord(level: Int, fineX: Int, fineZ: Int, teleport: Boolean) {
        rspAvatar.updateCoord(level, fineX, fineZ, teleport)
    }

    override fun updateAngle(angle: Int) {
        rspAvatar.updateAngle(angle)
    }

    override fun setSequence(seq: Int, delay: Int) {
        rspAvatar.extendedInfo.setSequence(seq, delay)
    }

    override fun setVisibleOps(ops: Byte) {
        rspAvatar.extendedInfo.setVisibleOps(ops)
    }

    override fun setSpecific(specific: Boolean) {
        rspAvatar.setSpecific(specific)
    }
}

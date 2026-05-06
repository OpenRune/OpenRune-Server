package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.camera.CamLookAt
import net.rsprot.protocol.game.outgoing.camera.CamLookAtV2
import net.rsprot.protocol.game.outgoing.camera.CamMoveTo
import net.rsprot.protocol.game.outgoing.camera.CamMoveToV2
import net.rsprot.protocol.game.outgoing.camera.CamReset
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public object Camera {
    public fun camReset(player: Player) {
        player.client.write(CamReset)
    }

    public fun camLookAt(player: Player, dest: CoordGrid, height: Int, rate: Int, rate2: Int) {
        player.client.write(CamLookAtV2(dest.x, dest.z, height, rate, rate2))
    }

    public fun camMoveTo(player: Player, dest: CoordGrid, height: Int, rate: Int, rate2: Int) {
        player.client.write(CamMoveToV2(dest.x, dest.z, height, rate, rate2))
    }
}

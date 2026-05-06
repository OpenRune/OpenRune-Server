package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.misc.player.SetMapFlagV2
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public fun Player.clearMapFlag() {
    client.write(SetMapFlagV2.RESET)
}

public object MapFlag {
    public fun setMapFlag(player: Player, coords: CoordGrid) {
        setMapFlag(player, coords.x, coords.z)
    }

    public fun setMapFlag(player: Player, x: Int, z: Int) {
        player.client.write(SetMapFlagV2(x, z))
    }
}

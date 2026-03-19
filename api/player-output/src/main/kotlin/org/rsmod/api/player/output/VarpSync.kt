package org.rsmod.api.player.output

import dev.openrune.types.varp.VarpServerType
import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

public object VarpSync {
    /** Calling this function directly will bypass [VarpType.transmit] conditions. */
    public fun writeVarp(player: Player, varp: VarpServerType, value: Int) {
        writeVarp(player.client, varp, value)
    }

    /** Calling this function directly will bypass [VarpType.transmit] conditions. */
    public fun writeVarp(client: Client<Any, Any>, varp: VarpServerType, value: Int) {
        val message =
            if (value in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                VarpSmall(varp.id, value)
            } else {
                VarpLarge(varp.id, value)
            }
        client.write(message)
    }
}

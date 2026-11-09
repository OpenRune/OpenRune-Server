package org.rsmod.api.player.output

import dev.openrune.types.varp.VarpServerType
import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpLong
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

public object VarpSync {
    /** Sends an integer varp directly, bypassing [VarpServerType.transmit]. */
    public fun writeVarp(player: Player, varp: VarpServerType, value: Int) {
        writeVarp(player.client, varp, value)
    }

    /**
     * Sends [value] with [VarpLong] without changing the player's stored varps.
     * The client stores long and integer varps separately. Use this overload for a long
     * varp even when [value] fits in an [Int].
     */
    public fun writeVarp(player: Player, varp: VarpServerType, value: Long) {
        writeVarp(player.client, varp, value)
    }

    /**
     * Writes [VarpLong] directly to [client]. The varp id must fit an unsigned short.
     * This bypasses [VarpServerType.transmit].
     */
    public fun writeVarp(client: Client<Any, Any>, varp: VarpServerType, value: Long) {
        require(varp.id in 0..65535) { "Varp id cannot be sent to the client: ${varp.id}" }
        client.write(VarpLong(varp.id, value))
    }

    /** Sends an integer varp directly, bypassing [VarpServerType.transmit]. */
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

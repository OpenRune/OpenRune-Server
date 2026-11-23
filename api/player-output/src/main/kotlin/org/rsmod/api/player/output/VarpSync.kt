package org.rsmod.api.player.output

import dev.openrune.types.varp.VarpServerType
import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpLong
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

public object VarpSync {
    /**
     * Sends [value] to the player's client as an integer varp update.
     *
     * This bypasses [VarpServerType.transmit] and leaves the player's stored varp value intact.
     * Use the [Long] overload for a varp stored in the client's long varp table.
     */
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

    /**
     * Writes an integer varp directly to [client], bypassing [VarpServerType.transmit].
     *
     * Values that fit in a signed byte use [VarpSmall]. Larger or more negative values
     * use [VarpLarge].
     */
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

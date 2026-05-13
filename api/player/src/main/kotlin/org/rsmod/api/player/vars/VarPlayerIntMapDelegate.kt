package org.rsmod.api.player.vars

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import org.rsmod.api.player.output.VarpSync
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerPersistenceHints
import org.rsmod.game.vars.VarPlayerIntMap
import org.rsmod.utils.bits.withBits

public class VarPlayerIntMapDelegate(
    private val client: Client<Any, Any>,
    private val vars: VarPlayerIntMap,
    private val engineLoggedIn: Boolean,
    private val player: Player,
) {

    public operator fun get(internal: String): Int {
        return vars[internal]
    }

    public operator fun set(internal: String, value: Int) {
        if(internal.startsWith("varp.")) {
            val varp = ServerCacheManager.getVarp(internal.asRSCM(RSCMType.VARP))?: return
            set(varp, value)
        } else {
            val varbit = ServerCacheManager.getVarbit(internal.asRSCM(RSCMType.VARBIT))?: return
            set(varbit, value)
        }
    }

    public operator fun set(varp: VarpServerType, value: Int) {
        val previous = vars.backing[varp.id]

        vars.backing[varp.id] = value

        if (engineLoggedIn && previous != value) {
            PlayerPersistenceHints.notify(player)
        }

        if (!engineLoggedIn) {
            return
        }

        val transmit = varp.transmit
        if (transmit.always) {
            VarpSync.writeVarp(client, varp, value)
        } else if (transmit.onDiff && previous != value) {
            VarpSync.writeVarp(client, varp, value)
        }
    }

    public operator fun set(varp: VarBitType, value: Int) {
        VarPlayerIntMap.assertVarBitBounds(varp, value)
        val mappedValue = vars[varp.baseVar]
        val packedValue = mappedValue.withBits(varp.bits, value)
        set(varp.baseVar, packedValue)
    }

    internal companion object {
        fun from(player: Player): VarPlayerIntMapDelegate {
            val engineLoggedIn = player.processedMapClock > 0
            return VarPlayerIntMapDelegate(
                client = player.client,
                vars = player.vars,
                engineLoggedIn = engineLoggedIn,
                player = player,
            )
        }
    }
}

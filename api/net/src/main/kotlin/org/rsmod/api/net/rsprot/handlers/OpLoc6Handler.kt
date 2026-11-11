package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.locs.OpLoc6V1
import net.rsprot.protocol.game.incoming.locs.OpLoc6V2
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

class OpLoc6Handler @Inject constructor() {
    val v1: MessageHandler<OpLoc6V1> = MessageHandler { player, message ->
        examine(player, message.id)
    }

    val v2: MessageHandler<OpLoc6V2> = MessageHandler { player, message ->
        examine(player, message.id)
    }

    private fun examine(player: Player, locId: Int) {
        val type = ServerCacheManager.getObject(locId) ?: return
        player.mes(type.desc, ChatType.LocExamine)
    }
}

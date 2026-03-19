package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import net.rsprot.protocol.game.incoming.npcs.OpNpc6
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

class OpNpc6Handler : MessageHandler<OpNpc6> {
    override fun handle(player: Player, message: OpNpc6) {
        val type = ServerCacheManager.getNpc(message.id) ?: return
        player.mes(type.examine, ChatType.NpcExamine)
    }
}

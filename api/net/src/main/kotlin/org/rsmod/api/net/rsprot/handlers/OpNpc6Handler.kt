package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.npcs.OpNpc6
import org.rsmod.api.player.events.NpcExamineEvent
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

class OpNpc6Handler @Inject constructor(private val eventBus: EventBus) : MessageHandler<OpNpc6> {
    override fun handle(player: Player, message: OpNpc6) {
        val type = ServerCacheManager.getNpc(message.id) ?: return
        player.mes(type.examine, ChatType.NpcExamine)
        eventBus.publish(NpcExamineEvent(player, type))
    }
}

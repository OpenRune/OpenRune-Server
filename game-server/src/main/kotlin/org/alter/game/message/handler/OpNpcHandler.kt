package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.npcs.OpNpc
import org.alter.game.model.move.PawnPathAction
import org.alter.game.message.MessageHandler
import org.alter.game.model.attr.CLIENT_KEY_COMBINATION
import org.alter.game.model.attr.INTERACTING_NPC_ATTR
import org.alter.game.model.attr.INTERACTING_OPT_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.impl.NpcAttackEvent
import org.alter.game.pluginnew.event.impl.NpcClickEvent
import java.lang.ref.WeakReference

class OpNpcHandler : MessageHandler<OpNpc> {
    override fun consume(
        client: Client,
        message: OpNpc,
    ) {
        val npc = client.world.npcs[message.index] ?: return
        log(client, "Npc option %d: index=%d, movement=%b, npc=%s", message.op, message.index, message.controlKey, npc)
        client.attr[CLIENT_KEY_COMBINATION] = if (message.controlKey) 2 else 0
        if (message.op == 2) {
            NpcAttackEvent(npc, client).post()
        } else {
            client.attr[INTERACTING_OPT_ATTR] = message.op
            client.attr[INTERACTING_NPC_ATTR] = WeakReference(npc)
            client.executePlugin(PawnPathAction.walkPlugin)
            NpcClickEvent(npc, MenuOption.fromId(message.op),client).post()
        }
    }
}

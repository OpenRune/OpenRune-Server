package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.objs.OpObjT
import org.alter.game.model.move.ObjectPathAction
import org.alter.game.message.MessageHandler
import org.alter.game.model.Tile
import org.alter.game.model.attr.INTERACTING_ITEM
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.model.entity.Entity
import org.alter.game.model.entity.Player
import org.alter.game.plugin.Plugin

class OpObjTHandler : MessageHandler<OpObjT> {
    override fun consume(
        client: Client,
        message: OpObjT,
    ) {
        /**
         * What to keep in mind:
         * This is used by:
         *  Item on ground item
         *  Spell on ground item
         *  Spell on Npc?
         *
         *  Could actually create global Interaction Range Attribute
         */
        val slot = message.selectedSub
        val sobj = message.selectedObj

        // If the tile is too far away, do nothing
        val tile = Tile(message.x, message.z, client.tile.height)
        log(
            client,
            "Item on object: item=%d, slot=%d, obj=%d, x=%d, y=%d",
            sobj,
            slot,
            message.id,
            message.x,
            message.z
        )



        val item = client.inventory[slot] ?: return
        client.executePlugin(itemOnObjectPlugin)
        //ItemOnGroundItemEvent(item, slot, message.id, tile, client).post()
    }

    val itemOnObjectPlugin: Plugin.() -> Unit = {
        val player = ctx as Player

        val item = player.attr[INTERACTING_ITEM]!!.get()!!
        val obj = player.attr[INTERACTING_OBJ_ATTR]!!.get()!!
        val lineOfSightRange = player.world.plugins.getObjInteractionDistance(obj.internalID)

        ObjectPathAction.walk(player, obj, lineOfSightRange) {
            if (!player.world.plugins.executeItemOnObject(player, obj.getTransform(player), item.id)) {
                player.writeMessage(Entity.NOTHING_INTERESTING_HAPPENS)
                if (player.world.devContext.debugObjects) {
                    player.writeMessage(
                        "Unhandled item on object: [item=$item, id=${obj.internalID}, type=${obj.type}, rot=${obj.rot}, x=${obj.tile.x}, y=${obj.tile.z}]",
                    )
                }
            }
        }
    }

}

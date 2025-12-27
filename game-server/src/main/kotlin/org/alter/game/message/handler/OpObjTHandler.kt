package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.objs.OpObjT
import org.alter.game.model.move.ObjectPathAction
import org.alter.game.message.MessageHandler
import org.alter.game.model.EntityType
import org.alter.game.model.Tile
import org.alter.game.model.attr.INTERACTING_ITEM
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.attr.INTERACTING_OPT_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.model.entity.Entity
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.move.ObjectPathAction.walk
import org.alter.game.plugin.Plugin
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.ItemOnGroundItemEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import java.lang.ref.WeakReference

class OpObjTHandler : MessageHandler<OpObjT> {
    override fun consume(
        client: Client,
        message: OpObjT,
    ) {

        val tile = Tile(message.x, message.z, client.tile.height)
        if (!tile.viewableFrom(client.tile, Player.TILE_VIEW_DISTANCE)) {
            return
        }

        /*
         * If player can't move, we don't do anything.
         */
        if (!client.lock.canMove()) {
            return
        }

        val slot = message.selectedSub
        val sobj = message.selectedObj


        val chunk = client.world.chunks.getOrCreate(tile)

        val obj = chunk.getEntities<GameObject>(tile).firstOrNull { it.internalID == message.id } ?: return

        client.attr[INTERACTING_OBJ_ATTR] = WeakReference(obj)

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

        val lineOfSightRange = client.world.plugins.getObjInteractionDistance(obj.internalID)

        walk(client, obj, lineOfSightRange) {
            val handledByNewSystem = EventManager.postWithResult( ItemOnGroundItemEvent(item, slot, message.id, tile, client))
            val handledByOldSystem = client.world.plugins.executeItemOnObject(client, obj.getTransform(client), item.id)

            if (!handledByNewSystem && !handledByOldSystem) {
                client.writeMessage(Entity.NOTHING_INTERESTING_HAPPENS)
                if (client.world.devContext.debugObjects) {
                    client.writeMessage(
                        "Unhandled item on object: [item=$item, id=${obj.internalID}, type=${obj.type}, rot=${obj.rot}, x=${obj.tile.x}, y=${obj.tile.z}]",
                    )
                }
            }
        }

    }
}

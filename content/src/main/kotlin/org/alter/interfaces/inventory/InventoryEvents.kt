package org.alter.interfaces.inventory

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.ext.message
import org.alter.game.action.EquipAction
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.UpdateInventory
import org.alter.game.model.item.Item
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ContainerType
import org.alter.game.pluginnew.event.impl.ItemClickEvent
import org.alter.game.pluginnew.event.impl.ItemDropEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfOpen
import org.alter.game.pluginnew.event.impl.onIfOverlayDrag
import org.alter.interfaces.ifClose
import org.alter.interfaces.ifSetEvents
import org.alter.invMoveToSlot
import kotlin.inv

class InventoryEvents : PluginEvent() {

    override fun init() {
        onIfOpen("interfaces.inventory") {

            onButton("components.inventory:items") { opHeldButton(player,op,item,slot, ContainerType.fromId(component.interfaceId)) }
            onIfOverlayDrag("components.inventory:items") { dragHeldButton(selectedSlot,targetSlot,player) }

            player.ifSetEvents(
                "components.inventory:items",
                0..27,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op6,
                IfEvent.Op7,
                IfEvent.Op10,
                IfEvent.TgtObj,
                IfEvent.TgtNpc,
                IfEvent.TgtLoc,
                IfEvent.TgtPlayer,
                IfEvent.TgtInv,
                IfEvent.TgtCom,
                IfEvent.Depth1,
                IfEvent.DragTarget,
                IfEvent.Target,
            )
        }
    }

    private fun opHeldButton(player : Player, op : MenuOption, item : Int, slot : Int,containerType : ContainerType?) {
        if (containerType == null) return

        when(op) {
            MenuOption.OP10 -> world.sendExamine(player, item, ExamineEntityType.ITEM)

            MenuOption.OP7 -> {
                val item = player.inventory[slot] ?: return
                if (world.plugins.canDropItem(player, item.id)) {
                    if (!world.plugins.executeItem(player, item.id, op.id)) {
                        val remove = player.inventory.remove(item.id,item.amount, assureFullRemoval = false, beginSlot = slot)
                        if (remove.completed > 0) {
                            val floor = GroundItem(item.id, remove.completed, player.tile, player)
                            floor.timeUntilPublic = world.gameContext.gItemPublicDelay
                            floor.timeUntilDespawn = world.gameContext.gItemDespawnDelay
                            floor.ownerShipType = 1
                            remove.firstOrNull()?.let { removed ->
                                floor.copyAttr(removed.item.attr)
                            }
                            world.spawn(floor)
                            ItemDropEvent(item.id, slot, containerType, player)
                        }
                    }
                }
            }

            MenuOption.OP3 -> {
                val item = player.inventory[slot] ?: return
                val result = EquipAction.equip(player, item, slot, ContainerType.INVENTORY)
                if (result == EquipAction.Result.UNHANDLED && world.devContext.debugItemActions) {
                    player.message("Unhandled item action: [item=${item}, slot=$slot, option=$op]")
                }
            }

            else -> ItemClickEvent(item, slot, op, containerType, player).post()
        }
    }

    private fun dragHeldButton(selectedSlot : Int?, targetSlot : Int?,player : Player) {
        val fromSlot = selectedSlot ?: return
        val intoSlot = targetSlot ?: return

        dragHeld(player,fromSlot, intoSlot)
    }

    private fun dragHeld(player: Player,fromSlot: Int, intoSlot: Int) {
        player.ifClose()
        player.invMoveToSlot(player.inventory, player.inventory, fromSlot, intoSlot)
    }

}
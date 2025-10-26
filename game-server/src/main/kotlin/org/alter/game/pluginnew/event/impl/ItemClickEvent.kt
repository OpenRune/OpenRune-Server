package org.alter.game.pluginnew.event.impl

import dev.openrune.ServerCacheManager.getItem
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption

open class ItemClickEvent(
    open val item: Int,
    open val op: MenuOption,
    val container: ContainerType,
    player: Player
) : EntityInteractionEvent<Int>(item, op, player) {

    override fun resolveOptionName(): String {
        val def = getItem(item) ?: error("Item not found for id=$item")
        return def.interfaceOptions.getOrNull(op.id) ?: error("No action found at index ${op.id} for item id=$item")
    }

    fun isContainer(type: ContainerType): Boolean = container == type

    fun isInventory(): Boolean = container == ContainerType.INVENTORY

    fun isWornEquipment(): Boolean = container == ContainerType.WORN_EQUIPMENT

    fun isEquipment(): Boolean = container == ContainerType.EQUIPMENT

}
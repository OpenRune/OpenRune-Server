package org.alter.game.pluginnew.event.impl

import dev.openrune.ServerCacheManager.getItem
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.rscm.RSCM.asRSCM

open class ItemClickEvent(
    open val item: Int,
    val slot : Int,
    open val op: MenuOption,
    val container: ContainerType,
    player: Player
) : EntityInteractionEvent<Int>(item, op, player) {

    public override fun resolveOptionName(): String {
        val def = getItem(item) ?: error("Item not found for id=$item")
        return def.interfaceOptions.getOrNull(op.id) ?: error("No action found at index ${op.id} for item id=$item")
    }

    fun isContainer(type: ContainerType): Boolean = container == type

    fun isInventory(): Boolean = container == ContainerType.INVENTORY

    fun isWornEquipment(): Boolean = container == ContainerType.WORN_EQUIPMENT

    fun isEquipment(): Boolean = container == ContainerType.EQUIPMENT

}

fun PluginEvent.onItemOption(
    item: String,
    option: String? = null,
    op: MenuOption? = null,
    action: suspend ItemClickEvent.() -> Unit
): EventListener<ItemClickEvent> {
    require(!(option != null && op != null)) { "You cannot provide both `option` and `op` at the same time." }

    val rscmItem = item.asRSCM()
    return on<ItemClickEvent> {
        where {
            this.item == rscmItem &&
                    (op == null || this.op == op) &&
                    (option == null || runCatching {
                        resolveOptionName().equals(option, ignoreCase = true) ||
                                this.op.name.equals(option, ignoreCase = true)
                    }.getOrDefault(false))
        }
        then { action(this) }
    }
}
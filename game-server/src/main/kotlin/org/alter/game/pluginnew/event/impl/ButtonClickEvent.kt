package org.alter.game.pluginnew.event.impl

import net.rsprot.protocol.util.CombinedId
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.rscm.RSCM.asRSCM

enum class ContainerType(val id: String) {
    INVENTORY("interfaces.inventory"),
    WORN_EQUIPMENT("interfaces.wornitems"),
    EQUIPMENT("interfaces.equipment"),

    UNKNOWN("unknown");

    companion object {
        fun fromId(id: Int): ContainerType = ContainerType.entries.find { it.id.asRSCM() == id } ?: UNKNOWN
    }
}

data class ButtonClickEvent(
    val component: CombinedId,
    val option: Int,
    val item: Int,
    val slot: Int,
    override val player: Player
) : PlayerEvent(player) {

    init {
        if (item != -1) {
            val containerType = ContainerType.fromId(component.interfaceId)
            val option = MenuOption.fromId(option)
            ItemClickEvent(item, slot, option, containerType, player).post()
        }
    }
}


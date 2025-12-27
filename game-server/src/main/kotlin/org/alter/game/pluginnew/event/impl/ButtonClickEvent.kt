package org.alter.game.pluginnew.event.impl

import dev.openrune.ServerCacheManager
import io.github.oshai.kotlinlogging.KotlinLogging
import net.rsprot.protocol.util.CombinedId
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.game.ui.IfSubType
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType

enum class ContainerType(val id: String) {
    INVENTORY("interfaces.inventory"),
    WORN_EQUIPMENT("interfaces.wornitems"),
    EQUIPMENT("interfaces.equipment"),
    SHOP("interfaces.shopmain"),
    BANK_SIDE("interfaces.bankside"),
    BANK("interfaces.bankmain");

    companion object {
        val logger = KotlinLogging.logger {}

        fun fromId(id: Int): ContainerType? {
            val entry = entries.find { it.id.asRSCM() == id }
            return entry ?: run {
                val reverseName = RSCM.getReverseMapping(RSCMType.INTERFACES, id)
                logger.info { "Missing mapping for id=$id (reverse: $reverseName) in ContainerType." }
                null
            }
        }
    }
}

data class ButtonClickEvent(
    val component: CombinedId,
    val option: Int,
    val item: Int,
    val slot: Int,
    val type : IfSubType,
    override val player: Player
) : PlayerEvent(player) {

    val op = MenuOption.fromId(option)
}

fun PluginEvent.onIfOverlayButton(
    componentID: String,
    action: suspend ButtonClickEvent.() -> Unit
): EventListener<ButtonClickEvent> {
    requireRSCM(RSCMType.COMPONENTS,componentID)
    return on<ButtonClickEvent> {
        where { component.combinedId == componentID.asRSCM() }
        then { action(this) }
    }
}

fun PluginEvent.onButton(
    componentID: String,
    action: suspend ButtonClickEvent.() -> Unit
): EventListener<ButtonClickEvent> = onIfModalButton(componentID,action)

fun PluginEvent.onIfModalButton(
    componentID: Int,
    action: suspend ButtonClickEvent.() -> Unit
): EventListener<ButtonClickEvent> {
    return on<ButtonClickEvent> {
        where { component.combinedId == componentID }
        then { action(this) }
    }
}


fun PluginEvent.onIfModalButton(
    componentID: String,
    action: suspend ButtonClickEvent.() -> Unit
): EventListener<ButtonClickEvent> {
    requireRSCM(RSCMType.COMPONENTS,componentID)
    return on<ButtonClickEvent> {
        where { component.combinedId == componentID.asRSCM()}
        then { action(this) }
    }
}

data class EquipToSlot(
    val component: CombinedId,
    val option: Int,
    val item: Int,
    val slot: Int,
    val type : IfSubType,
    override val player: Player
) : PlayerEvent(player) {

    init {

        if (item != -1) {
            val containerType = ContainerType.fromId(component.interfaceId)
            val menuOption = MenuOption.fromId(option)

            if (containerType != null) {
                when (menuOption) {
                    MenuOption.OP7 -> ItemDropEvent(item, slot, containerType, player).post()
                    MenuOption.OP10 -> ExamineEvent(item, ExamineEntityType.ITEM, player).post()
                    else -> {
                        println("ITEM: ${item}")
                        ItemClickEvent(item, slot, menuOption, containerType, player).post()
                    }
                }
            }
        }
    }
}

fun PluginEvent.onEquipToSlot(
    wp: Int,
    action: suspend EquipToSlot.() -> Unit
): EventListener<EquipToSlot> {
    return on<EquipToSlot> {
        where { false }
        then { action(this) }
    }
}

package org.alter.game.pluginnew.event.impl

import dev.openrune.ServerCacheManager.getObject
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.PlayerEvent

class ItemOnObject(
    val item : Item,
    val gameObject: GameObject,
    val slot : Int,
    override val player: Player
) : PlayerEvent(player) {

    val option: String
        get() = resolveOptionName(gameObject.internalID, MenuOption.OP1.id)

    companion object {
        private fun resolveOptionName(id: Int, opId: Int): String {
            val def = getObject(id)
                ?: error("Object not found for id=$id")

            return def.actions.getOrNull(opId - 1)
                ?: error("No action found at index $opId for object id=$id")
        }
    }
}

open class ObjectClickEvent(
    open val gameObject: GameObject,
    open val op: MenuOption,
    player: Player
) : EntityInteractionEvent<GameObject>(gameObject, op, player) {

    val id : Int = gameObject.internalID

    override fun resolveOptionName(): String {
        val def = getObject(gameObject.internalID) ?: error("Object not found for id=${gameObject.id}[${gameObject.id}]")
        return def.actions.getOrNull(op.id - 1) ?: error("No action found at index ${op.id} for object id=${gameObject.id}[${gameObject.id}]")
    }
}
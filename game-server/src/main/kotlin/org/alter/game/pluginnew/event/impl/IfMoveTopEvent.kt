package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType

data class IfMoveTopEvent(
    val type: String,
    override val player: Player
) : PlayerEvent(player)

data class IfMoveSubEvent(
    val type: String,
    override val player: Player
) : PlayerEvent(player)

fun PluginEvent.onIfMoveTop(
    type: String,
    action: suspend IfMoveTopEvent.() -> Unit
): EventListener<IfMoveTopEvent> {
    requireRSCM(RSCMType.INTERFACES,type)
    return on<IfMoveTopEvent> {
        where { this.type == type }
        then { action(this) }
    }
}

fun PluginEvent.onIfMoveSub(
    type: String,
    action: suspend IfMoveSubEvent.() -> Unit
): EventListener<IfMoveSubEvent> {
    return on<IfMoveSubEvent> {
        where { this.type == type }
        then { action(this) }
    }
}

fun PluginEvent.onIfMoveTop(
    type: Int,
    action: suspend IfMoveTopEvent.() -> Unit
): EventListener<IfMoveTopEvent> {
    return on<IfMoveTopEvent> {
        where { this.type.asRSCM() == type }
        then { action(this) }
    }
}
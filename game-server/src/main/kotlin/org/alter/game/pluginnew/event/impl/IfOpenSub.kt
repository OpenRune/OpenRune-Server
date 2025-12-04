package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.game.ui.IfSubType

public typealias OpenSub = IfOpenSub

data class IfOpenSub(
    override val player: Player,
    val interf: String,
    val target: String,
    val subType: IfSubType,
) : PlayerEvent(player)

fun PluginEvent.onIfOpen(
    type: String,
    action: suspend IfOpenSub.() -> Unit
): EventListener<IfOpenSub> {
    return on<IfOpenSub> {
        where { this.interf == type }
        then { action(this) }
    }
}
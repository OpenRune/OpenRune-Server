package org.alter.game.pluginnew.event.impl

import dev.openrune.definition.type.widget.Component
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.game.ui.UserInterface
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType


class CloseSubEvent(
    val interf: UserInterface,
    val from: Component,
    player: Player
) : PlayerEvent(player)

fun PluginEvent.onIfClose(
    interfaceID: String,
    action: suspend CloseSubEvent.() -> Unit
): EventListener<CloseSubEvent> {
    RSCM.requireRSCM(RSCMType.INTERFACES, interfaceID)
    return on<CloseSubEvent> {
        where { interf.id == interfaceID.asRSCM() }
        then { action(this) }
    }
}

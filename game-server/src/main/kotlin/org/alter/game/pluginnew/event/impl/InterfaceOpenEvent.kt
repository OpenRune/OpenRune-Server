package org.alter.game.pluginnew.event.impl

import net.rsprot.protocol.util.CombinedId
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType

class InterfaceOpenEvent(player: Player, val interfaceID: Int) : PlayerEvent(player)

fun PluginEvent.onInterfaceOpen(
    infId: Int,
    action: suspend InterfaceOpenEvent.() -> Unit
): EventListener<InterfaceOpenEvent> =
    on<InterfaceOpenEvent> {
        where { infId == interfaceID }
        then { action(this) }
    }

fun PluginEvent.onInterfaceOpen(
    infId: String,
    action: suspend InterfaceOpenEvent.() -> Unit
): EventListener<InterfaceOpenEvent> =
    on<InterfaceOpenEvent> {
        requireRSCM(RSCMType.INTERFACES, infId)
        where { infId.asRSCM() == interfaceID }
        then { action(this) }
    }

class InterfaceCloseEvent(player: Player, val interfaceID: Int) : PlayerEvent(player)

fun PluginEvent.onInterfaceClose(
    infId: Int,
    action: suspend InterfaceCloseEvent.() -> Unit
): EventListener<InterfaceCloseEvent> =
    on<InterfaceCloseEvent> {
        where { infId == interfaceID }
        then { action(this) }
    }

fun PluginEvent.onInterfaceClose(
    infId: String,
    action: suspend InterfaceCloseEvent.() -> Unit
): EventListener<InterfaceCloseEvent> =
    on<InterfaceCloseEvent> {
        requireRSCM(RSCMType.INTERFACES, infId)
        where { infId.asRSCM() == interfaceID }
        then { action(this) }
    }
package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType

class IfOverlayDrag(
    public val selectedSlot: Int?,
    public val selectedObj: Int?,
    public val targetSlot: Int?,
    public val targetObj: Int?,
    val selectedComponent: Int,
    val targetComponent: Int,
    player: Player
) : PlayerEvent(player)

fun PluginEvent.onIfOverlayDrag(
    selected: String,
    action: suspend IfOverlayDrag.() -> Unit
): EventListener<IfOverlayDrag> {
    requireRSCM(RSCMType.COMPONENTS,selected)
    return on<IfOverlayDrag> {
        where { selectedComponent == selected.asRSCM() }
        then { action(this) }
    }
}

class IfModalDrag(
    public val selectedSlot: Int?,
    public val selectedObj: Int?,
    public val targetSlot: Int?,
    public val targetObj: Int?,
    val selectedComponent: Int,
    val targetComponent: Int,
    player: Player
) : PlayerEvent(player)

fun PluginEvent.onIfModalDrag(
    selected: String,
    action: suspend IfModalDrag.() -> Unit
): EventListener<IfModalDrag> {
    requireRSCM(RSCMType.COMPONENTS,selected)
    return on<IfModalDrag> {
        where { selectedComponent == selected.asRSCM() }
        then { action(this) }
    }
}

fun PluginEvent.onIfModalDrag(
    selected: String,
    target: String,
    action: suspend IfModalDrag.() -> Unit
): EventListener<IfModalDrag> {
    requireRSCM(RSCMType.COMPONENTS,selected)
    requireRSCM(RSCMType.COMPONENTS,target)
    return on<IfModalDrag> {
        where { selectedComponent == selected.asRSCM() && targetComponent == target.asRSCM() }
        then { action(this) }
    }
}
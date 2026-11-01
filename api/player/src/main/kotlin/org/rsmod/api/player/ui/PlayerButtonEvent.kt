package org.rsmod.api.player.ui

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Published when a player clicks an interface component that has a handler, just before the handler
 * runs. Says what the player clicked, not that it worked: the handler may refuse, and a click that
 * the server blocks (a modal button while the player is busy) is never published.
 *
 * @param comsub the clicked slot within the component, or `-1` for components without slots.
 * @param obj the item in the clicked slot, if any.
 * @param modal the click was on a modal interface rather than an overlay or the gameframe.
 */
public data class PlayerButtonEvent(
    override val player: Player,
    public val component: ComponentType,
    public val comsub: Int,
    public val obj: ItemServerType?,
    public val op: IfButtonOp,
    public val modal: Boolean,
) : UnboundEvent, PlayerEvent

public fun EventBus.observeButton(player: Player, event: IfOverlayButton) {
    if (contains(IfOverlayButton::class.java, event.id)) {
        publishButton(player, event.component, event.comsub, event.obj, event.op, modal = false)
    }
}

public fun EventBus.observeButton(player: Player, event: IfModalButton) {
    if (contains(IfModalButton::class.java, event.id)) {
        publishButton(player, event.component, event.comsub, event.obj, event.op, modal = true)
    }
}

private fun EventBus.publishButton(
    player: Player,
    component: ComponentType,
    comsub: Int,
    obj: ItemServerType?,
    op: IfButtonOp,
    modal: Boolean,
) {
    if (unbound[PlayerButtonEvent::class.java] == null) {
        return
    }
    publish(PlayerButtonEvent(player, component, comsub, obj, op, modal))
}

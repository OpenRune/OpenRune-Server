package org.rsmod.api.player.events.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.obj.Obj

/** What a player interacted with. Locs and npcs carry the type the player actually saw. */
public sealed class ObservedTarget {
    public data class Loc(val loc: BoundLocInfo, val type: ObjectServerType) : ObservedTarget()

    public data class Npc(val npc: org.rsmod.game.entity.Npc, val type: NpcServerType) :
        ObservedTarget()

    public data class Ground(val obj: Obj) : ObservedTarget()

    public data class Other(val player: Player) : ObservedTarget()

    /** An item in the inventory that an option was chosen on, or that another item was used on. */
    public data class Held(val type: ItemServerType, val slot: Int) : ObservedTarget()
}

public sealed class ObservedAction {
    /** A menu option, `1..5`. */
    public data class Option(val slot: Int) : ObservedAction()

    /**
     * An item or spell used on the target. [item] is null for spells; [component] is null when one
     * inventory item is used on another, where [comsub] is the used item's slot. [count] is how many
     * of [item] that inventory slot held when the interaction started, 0 for spells.
     */
    public data class Use(
        val item: ItemServerType?,
        val component: ComponentType?,
        val comsub: Int,
        val count: Int,
    ) : ObservedAction()
}

/**
 * Published when an interaction handler is about to run. Says what the player did, not that it
 * worked: the handler may refuse on a requirement or a full inventory.
 *
 * Not published for loc, npc, ground and player interactions that have no handler. Held-item
 * options are published whenever the item has that option, implemented or not.
 */
public data class PlayerInteractionEvent(
    override val player: Player,
    public val target: ObservedTarget,
    public val action: ObservedAction,
) : UnboundEvent, PlayerEvent

/**
 * Published when the handler behind [started] returns. [completed] is false if it was interrupted
 * (the player moved away, started something else, ...); a dialogue or a skilling loop only
 * completes once it ends.
 */
public data class PlayerInteractionFinishedEvent(
    override val player: Player,
    public val started: PlayerInteractionEvent,
    public val completed: Boolean,
) : UnboundEvent, PlayerEvent

public fun Inventory.heldCount(type: ItemServerType?, slot: Int): Int {
    val obj = this[slot] ?: return 0
    return if (type != null && obj.id == type.id) obj.count else 0
}

/**
 * Runs [block] and publishes [PlayerInteractionEvent] before it and
 * [PlayerInteractionFinishedEvent] after. [event] is only built when something listens.
 */
public suspend fun EventBus.observeInteraction(
    event: () -> PlayerInteractionEvent,
    block: suspend () -> Unit,
) {
    val watched =
        unbound[PlayerInteractionEvent::class.java] != null ||
            unbound[PlayerInteractionFinishedEvent::class.java] != null
    if (!watched) {
        block()
        return
    }
    val started = event()
    publish(started)
    var completed = false
    try {
        block()
        completed = true
    } finally {
        publish(PlayerInteractionFinishedEvent(started.player, started, completed))
    }
}

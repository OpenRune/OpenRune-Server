package org.rsmod.api.invtx

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.TransactionInventory
import org.rsmod.objtx.TransactionObj

/** [delta] more (or fewer, if negative) of [obj] in [inv]. */
public data class InvDelta(val inv: Inventory, val obj: Int, val delta: Int)

/**
 * Published after a transaction that changed one of the player's inventories is committed.
 *
 * Only transactions are seen: items stored in virtual storage (a coal bag) and inventories rebuilt
 * on login or world type switch never appear. A transaction that is never committed publishes
 * nothing. Objs are compared by id, so noting or un-noting an item shows as one id lost and the
 * other gained.
 */
public class PlayerInvChangedEvent(
    override val player: Player,
    public val deltas: List<InvDelta>,
) : UnboundEvent, PlayerEvent {
    /**
     * What the player gained on balance across all their inventories in this transaction, by obj
     * id. Moving an item between inventories (bank, equipment) or swapping it nets to nothing and
     * is left out, so this is what actually entered the player's possession.
     */
    public val acquired: Map<Int, Int> by lazy {
        val totals = Int2LongOpenHashMap()
        for (delta in deltas) {
            totals.addTo(delta.obj, delta.delta.toLong())
        }
        buildMap {
            for (entry in totals.int2LongEntrySet()) {
                if (entry.longValue > 0) put(entry.intKey, entry.longValue.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }
        }
    }
}

internal var cachedEventBus: EventBus? = null

private var publishing = false

/**
 * Null unless something listens, and while a listener is itself running: a handler that adds items
 * does not publish again, so it cannot trigger itself.
 */
internal fun Player.invChangeInterceptor():
    ((List<TransactionInventory<InvObj>>, () -> Unit) -> Unit)? {
    val bus = cachedEventBus ?: return null
    if (publishing || bus.unbound[PlayerInvChangedEvent::class.java] == null) {
        return null
    }
    return { inventories, commit ->
        val deltas = deltas(inventories)
        commit()
        if (deltas.isNotEmpty()) {
            publishing = true
            try {
                bus.publish(PlayerInvChangedEvent(this, deltas))
            } finally {
                publishing = false
            }
        }
    }
}

private fun Player.deltas(inventories: List<TransactionInventory<InvObj>>): List<InvDelta> {
    val result = ArrayList<InvDelta>()
    for (transactionInv in inventories) {
        val inv = invMap.values.firstOrNull { it.objs === transactionInv.output } ?: continue
        val totals = invDeltas(transactionInv.output, transactionInv.image)
        for (entry in totals.int2LongEntrySet()) {
            val delta = entry.longValue.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            result += InvDelta(inv, entry.intKey, delta.toInt())
        }
    }
    return result
}

/** Net change per obj id between a live inventory and its pending transaction image. */
internal fun invDeltas(before: Array<InvObj?>, after: Array<TransactionObj?>): Int2LongOpenHashMap {
    val totals = Int2LongOpenHashMap()
    for (slot in before.indices) {
        val old = before[slot]
        val new = after[slot]
        if (old?.id == new?.id && old?.count == new?.count) {
            continue
        }
        if (old != null) {
            totals.addTo(old.id, -old.count.toLong())
        }
        if (new != null) {
            totals.addTo(new.id, new.count.toLong())
        }
    }
    totals.values.removeIf { it == 0L }
    return totals
}

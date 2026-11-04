package org.rsmod.api.delivery

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerPersistenceHints

/**
 * Persist timed parcels and accumulated stores per player. Commit arrivals before callbacks;
 * unknown line ids retain their saved contents.
 */
@Singleton
public class DeliveryManager
@Inject
constructor(private val registry: DeliveryLineRegistry, private val clock: DeliveryClock) {
    private val logger = InlineLogger()

    private val lastWarnCycle: MutableMap<String, Int> = HashMap()

    /* ------------------------------------------------------------------ enqueue */

    /**
     * Schedules [amount] of [payload] to arrive [DeliveryLine.delayTicks] cycles from now.
     *
     * Returns `false` - having logged it - when [amount] is not positive, when [line] is not
     * registered, or when the player already has [DeliveryLine.maxPending] parcels in transit on it.
     * None of the three throws: an op handler is not a good place to raise, and `amount` is
     * routinely computed from something the player controls.
     */
    public fun enqueue(
        player: Player,
        line: DeliveryLine,
        payload: Int,
        amount: Int = 1,
    ): Boolean {
        if (amount <= 0) {
            warn("amount:${line.id}") {
                "Delivery enqueue needs a positive `amount`: $amount (line=$line, player=$player)"
            }
            return false
        }
        if (!registry.isRegistered(line)) {
            warn("unregistered:${line.id}") {
                "Delivery line is not registered; parcel dropped. (line=$line, player=$player)"
            }
            return false
        }
        val state = liveOrCreate(player)
        val lineState = state.line(line.id)
        if (line.pauseWhileFrozen && lineState.lastPauseCycle == null) {
            lineState.lastPauseCycle = clock.cycle
        }
        if (lineState.pending.size >= line.maxPending) {
            warn("maxpending:${line.id}") {
                "Delivery line is at `maxPending`; parcel dropped. " +
                    "(line=$line, pending=${lineState.pending.size}, player=$player)"
            }
            return false
        }
        lineState.pending.add(PendingDelivery(payload, amount, clock.cycle + line.delayTicks))
        lineState.recomputeNextDue()
        state.recomputeNextDue()
        flush(player, state)
        return true
    }

    public fun enqueueAll(player: Player, line: DeliveryLine, payloads: Iterable<Int>): Int {
        if (!registry.isRegistered(line)) {
            warn("unregistered:${line.id}") {
                "Delivery line is not registered; batch dropped. (line=$line, player=$player)"
            }
            return 0
        }
        val state = liveOrCreate(player)
        val lineState = state.line(line.id)
        if (line.pauseWhileFrozen && lineState.lastPauseCycle == null) {
            lineState.lastPauseCycle = clock.cycle
        }
        var accepted = 0
        for (payload in payloads) {
            if (lineState.pending.size >= line.maxPending) {
                warn("maxpending:${line.id}") {
                    "Delivery line is at `maxPending`; rest of batch dropped. " +
                        "(line=$line, pending=${lineState.pending.size}, player=$player)"
                }
                break
            }
            lineState.pending.add(PendingDelivery(payload, 1, clock.cycle + line.delayTicks))
            accepted++
        }
        if (accepted > 0) {
            lineState.recomputeNextDue()
            state.recomputeNextDue()
            flush(player, state)
        }
        return accepted
    }

    public fun pendingCount(player: Player, line: DeliveryLine): Int =
        lineState(player, line)?.pendingTotal() ?: 0

    public fun pendingParcels(player: Player, line: DeliveryLine): List<DeliveryParcel> {
        val lineState = lineState(player, line) ?: return emptyList()
        return lineState.pending.map {
            DeliveryParcel(it.payload, it.amount, (it.dueCycle - clock.cycle).coerceAtLeast(0))
        }
    }

    public fun storedCount(player: Player, line: DeliveryLine): Int =
        lineState(player, line)?.storedTotal() ?: 0

    public fun storedSnapshot(player: Player, line: DeliveryLine): Map<Int, Int> {
        val lineState = lineState(player, line) ?: return emptyMap()
        return LinkedHashMap(lineState.stored)
    }

    public fun storedCountOf(player: Player, line: DeliveryLine, payload: Int): Int =
        lineState(player, line)?.stored?.get(payload) ?: 0

    public fun committedCount(player: Player, line: DeliveryLine): Int {
        val lineState = lineState(player, line) ?: return 0
        return lineState.storedTotal() + lineState.pendingTotal()
    }

    public fun capacity(player: Player, line: DeliveryLine): Int = evalCapacity(player, line)

    public fun freeCapacity(player: Player, line: DeliveryLine): Int =
        (evalCapacity(player, line) - storedCount(player, line)).coerceAtLeast(0)

    public fun isFull(player: Player, line: DeliveryLine): Boolean =
        storedCount(player, line) >= evalCapacity(player, line)

    public fun takeStored(
        player: Player,
        line: DeliveryLine,
        payload: Int,
        amount: Int,
    ): Int {
        if (amount <= 0) {
            return 0
        }
        val state = player.attr[DeliveryAttributes.LIVE] ?: return 0
        val lineState = state.lines[line.id] ?: return 0
        val held = lineState.stored[payload] ?: return 0
        val taken = minOf(held, amount)
        if (taken <= 0) {
            return 0
        }
        if (taken == held) {
            lineState.stored.remove(payload)
        } else {
            lineState.stored[payload] = held - taken
        }
        flush(player, state)
        return taken
    }

    public fun takeAllStored(player: Player, line: DeliveryLine): Map<Int, Int> {
        val state = player.attr[DeliveryAttributes.LIVE] ?: return emptyMap()
        val lineState = state.lines[line.id] ?: return emptyMap()
        if (lineState.stored.isEmpty()) {
            return emptyMap()
        }
        val taken = LinkedHashMap(lineState.stored)
        lineState.stored.clear()
        flush(player, state)
        return taken
    }

    public fun cancelPending(player: Player, line: DeliveryLine): List<DeliveryParcel> {
        val state = player.attr[DeliveryAttributes.LIVE] ?: return emptyList()
        val lineState = state.lines[line.id] ?: return emptyList()
        if (lineState.pending.isEmpty()) {
            return emptyList()
        }
        val cancelled =
            lineState.pending.map {
                DeliveryParcel(it.payload, it.amount, (it.dueCycle - clock.cycle).coerceAtLeast(0))
            }
        lineState.pending = ArrayList()
        lineState.recomputeNextDue()
        state.recomputeNextDue()
        flush(player, state)
        return cancelled
    }

    /* ------------------------------------------------------------- tick and hooks */

    /**
     * Delivers everything due for [player]. Called once per player per `GameLifecycle.LateCycle`.
     *
     * The idle path is one attribute lookup and one integer comparison. An exception anywhere inside
     * is contained here so one player's broken line cannot abort the drain for everyone else.
     */
    internal fun drain(player: Player) {
        val state = player.attr[DeliveryAttributes.LIVE] ?: return
        pauseFrozenLines(player, state)
        if (clock.cycle < state.nextDueCycle) {
            return
        }
        try {
            drainDue(player, state)
        } catch (e: Exception) {
            warn("drain", e) { "Could not drain deliveries for player: $player." }
        } catch (e: NotImplementedError) {
            warn("drain", e) { "Could not drain deliveries for player: $player." }
        }
    }

    private fun pauseFrozenLines(player: Player, state: DeliveryState) {
        var paused = false
        for ((lineId, lineState) in state.lines) {
            val line = registry.find(lineId) ?: continue
            if (!line.pauseWhileFrozen) continue
            val previousCycle = lineState.lastPauseCycle ?: clock.cycle
            lineState.lastPauseCycle = clock.cycle
            if (lineState.pending.isEmpty() || !evalFrozen(line)) continue
            val elapsed = clock.cycle - previousCycle
            if (elapsed <= 0) continue
            lineState.pausePending(elapsed)
            paused = true
        }
        if (paused) {
            state.recomputeNextDue()
            flush(player, state)
        }
    }

    private fun drainDue(player: Player, state: DeliveryState) {
        var delivered = false
        var batchCompleted = false
        // Snapshot the ids: an arrival handler is allowed to enqueue, which can add a line.
        for (lineId in state.lines.keys.toList()) {
            val lineState = state.lines[lineId] ?: continue
            if (clock.cycle < lineState.nextDueCycle) {
                continue
            }
            // An unregistered line is left completely untouched - parcels and store both - so an
            // unloaded content module cannot cost a player their goods.
            val line = registry.find(lineId) ?: continue

            // Pause-aware lines have already shifted their deadlines above.
            if (evalFrozen(line)) {
                continue
            }

            val arrived = ArrayList<PendingDelivery>()
            val remaining = ArrayList<PendingDelivery>(lineState.pending.size)
            for (parcel in lineState.pending) {
                if (clock.cycle >= parcel.dueCycle) {
                    arrived += parcel
                } else {
                    remaining += parcel
                }
            }
            if (arrived.isEmpty()) {
                lineState.recomputeNextDue()
                continue
            }
            // Committed before any handler runs, so re-entrant calls and thrown handlers both see
            // a consistent queue.
            lineState.pending = remaining
            lineState.recomputeNextDue()
            state.recomputeNextDue()

            for (parcel in arrived) {
                deliver(player, line, lineState, parcel)
            }
            delivered = true
            if (lineState.pending.isEmpty()) {
                batchCompleted = true
            }
        }
        if (!delivered) {
            return
        }
        flush(player, state)
        if (batchCompleted) {
            // The drain runs outside `PlayerPersistenceHints.enter`, so the flush above raises no
            // autosave hint of its own - deliberately, or a 28-parcel batch would ask for 28 saves.
            // Asking once, when a batch finishes, is the useful middle: "I deposited and then the
            // server died" loses nothing, and a steady conveyor still only saves once per batch.
            PlayerPersistenceHints.notify(player)
        }
    }

    private fun deliver(
        player: Player,
        line: DeliveryLine,
        lineState: DeliveryLineState,
        parcel: PendingDelivery,
    ) {
        val capacity = evalCapacity(player, line)
        val free = (capacity - lineState.storedTotal()).coerceAtLeast(0)
        val stored = minOf(parcel.amount, free)
        val discarded = parcel.amount - stored
        if (stored > 0) {
            lineState.stored[parcel.payload] = (lineState.stored[parcel.payload] ?: 0) + stored
        }
        val arrival =
            DeliveryArrival(
                payload = parcel.payload,
                amount = parcel.amount,
                stored = stored,
                discarded = discarded,
                storedTotal = lineState.storedTotal(),
            )
        try {
            line.onArrival(player, arrival)
        } catch (e: Exception) {
            warn("arrival:${line.id}", e) {
                "Delivery arrival handler failed. (line=$line, $arrival, player=$player)"
            }
        } catch (e: NotImplementedError) {
            warn("arrival:${line.id}", e) {
                "Delivery arrival handler failed. (line=$line, $arrival, player=$player)"
            }
        }
    }

    /**
     * Rebuilds the live state from the saved document. Called from `onPlayerLogin`, which runs in
     * the pre-tick phase, so anything already overdue is delivered by this same loop's drain.
     *
     * A document that cannot be decoded is left **exactly as loaded**: no live state is created, so
     * nothing ever overwrites it, and the account save re-persists the same bytes. The player is a
     * login they can complete, not a sack that gets deleted by a parse bug.
     */
    internal fun rehydrate(player: Player) {
        val raw: Any? = player.attr[DeliveryAttributes.DOCUMENT]
        val document = raw as? Map<*, *> ?: return
        if (document.isEmpty()) {
            player.attr.remove(DeliveryAttributes.DOCUMENT)
            return
        }
        val state =
            try {
                DeliveryCodec.decode(document, clock.cycle, System.currentTimeMillis())
            } catch (e: Exception) {
                logger.error(e) {
                    "Could not decode delivery document; leaving it untouched. " +
                        "(player=$player, document=${document.toString().take(MAX_LOGGED_LENGTH)})"
                }
                return
            }
        if (state.isEmpty) {
            player.attr.remove(DeliveryAttributes.DOCUMENT)
            return
        }
        player.attr[DeliveryAttributes.LIVE] = state
    }

    /**
     * Re-stamps the document so every parcel's wall-clock instant is measured from *now* rather than
     * from whenever it was last written. Called from `onPlayerLogout`, which
     * `PlayerLogoutProcessor` publishes before `accountRegistry.queueLogout`, so the refreshed
     * document is the one that gets saved.
     */
    internal fun flushForSave(player: Player) {
        val state = player.attr[DeliveryAttributes.LIVE] ?: return
        flush(player, state)
    }

    private fun liveOrCreate(player: Player): DeliveryState =
        player.attr.getOrPut(DeliveryAttributes.LIVE) { DeliveryState() }

    private fun lineState(player: Player, line: DeliveryLine): DeliveryLineState? =
        player.attr[DeliveryAttributes.LIVE]?.lines?.get(line.id)

    /**
     * Copy-on-write: a brand new document is built and assigned, never edited in place, so the
     * account saver thread can only ever serialise a complete one.
     */
    private fun flush(player: Player, state: DeliveryState) {
        state.pruneEmpty()
        state.recomputeNextDue()
        for ((id, lineState) in state.lines) {
            val line = registry.find(id) ?: continue
            lineState.pausedAtSave = line.pauseWhileFrozen && evalFrozen(line)
        }
        val document = DeliveryCodec.encode(state, clock.cycle, System.currentTimeMillis())
        if (document.isEmpty()) {
            player.attr.remove(DeliveryAttributes.DOCUMENT)
        } else {
            player.attr[DeliveryAttributes.DOCUMENT] = document
        }
    }

    private fun evalFrozen(line: DeliveryLine): Boolean =
        try {
            line.frozen()
        } catch (e: Exception) {
            warn("frozen:${line.id}", e) {
                "Delivery frozen lambda failed; treating as not frozen. (line=$line)"
            }
            false
        } catch (e: NotImplementedError) {
            warn("frozen:${line.id}", e) {
                "Delivery frozen lambda failed; treating as not frozen. (line=$line)"
            }
            false
        }

    private fun evalCapacity(player: Player, line: DeliveryLine): Int =
        try {
            line.capacity(player).coerceAtLeast(0)
        } catch (e: Exception) {
            warn("capacity:${line.id}", e) {
                "Delivery capacity lambda failed; treating as full. (line=$line, player=$player)"
            }
            0
        } catch (e: NotImplementedError) {
            warn("capacity:${line.id}", e) {
                "Delivery capacity lambda failed; treating as full. (line=$line, player=$player)"
            }
            0
        }

    private inline fun warn(key: String, cause: Throwable? = null, message: () -> String) {
        val last = lastWarnCycle[key]
        if (last != null && clock.cycle - last in 0 until WARN_INTERVAL_CYCLES) {
            return
        }
        lastWarnCycle[key] = clock.cycle
        logger.error(cause) { message() }
    }

    private companion object {
        private const val WARN_INTERVAL_CYCLES: Int = 100

        private const val MAX_LOGGED_LENGTH: Int = 8192
    }
}

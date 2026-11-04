package org.rsmod.api.delivery

/**
 * One player's live delivery state: every line they have ever used, keyed by [DeliveryLine.id].
 *
 * [nextDueCycle] is the whole point of this class's bookkeeping. The drain runs for every online
 * player every tick, so the idle case has to be one integer comparison; keeping the minimum due
 * cycle cached here means a player with nothing in transit costs one attribute lookup and one
 * compare, and a player with a full sack but no parcels costs the same.
 */
internal class DeliveryState {
    val lines: MutableMap<String, DeliveryLineState> = LinkedHashMap()

    var nextDueCycle: Int = Int.MAX_VALUE
        private set

    fun line(id: String): DeliveryLineState = lines.getOrPut(id) { DeliveryLineState() }

    val isEmpty: Boolean
        get() = lines.values.all(DeliveryLineState::isEmpty)

    fun recomputeNextDue() {
        var next = Int.MAX_VALUE
        for (state in lines.values) {
            if (state.nextDueCycle < next) {
                next = state.nextDueCycle
            }
        }
        nextDueCycle = next
    }

    fun pruneEmpty() {
        val iterator = lines.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.isEmpty) {
                iterator.remove()
            }
        }
    }
}

internal class DeliveryLineState {
    var lastPauseCycle: Int? = null
    var pausedAtSave: Boolean = false

    fun pausePending(ticks: Int) {
        if (ticks <= 0 || pending.isEmpty()) return
        pending = pending.mapTo(ArrayList(pending.size)) {
            PendingDelivery(it.payload, it.amount, it.dueCycle + ticks)
        }
        recomputeNextDue()
    }

    /**
     * Replaced wholesale rather than mutated during a drain, so a re-entrant [DeliveryManager]
     * call from an arrival handler cannot corrupt the iteration.
     */
    var pending: MutableList<PendingDelivery> = ArrayList()

    val stored: MutableMap<Int, Int> = LinkedHashMap()

    var nextDueCycle: Int = Int.MAX_VALUE
        private set

    val isEmpty: Boolean
        get() = pending.isEmpty() && stored.isEmpty()

    fun storedTotal(): Int {
        var total = 0
        for (amount in stored.values) {
            total += amount
        }
        return total
    }

    fun pendingTotal(): Int {
        var total = 0
        for (parcel in pending) {
            total += parcel.amount
        }
        return total
    }

    fun recomputeNextDue() {
        var next = Int.MAX_VALUE
        for (parcel in pending) {
            if (parcel.dueCycle < next) {
                next = parcel.dueCycle
            }
        }
        nextDueCycle = next
    }
}

internal class PendingDelivery(val payload: Int, val amount: Int, val dueCycle: Int)

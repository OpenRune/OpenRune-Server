package org.rsmod.api.delivery

/** Persist wall-clock deadlines, or remaining ticks while a physical conveyor is stopped. */
internal object DeliveryCodec {
    const val PENDING: String = "pending"
    const val STORED: String = "stored"
    const val PAYLOAD: String = "payload"
    const val AMOUNT: String = "amount"
    const val DUE: String = "due"
    const val PAUSED: String = "paused"
    const val REMAINING: String = "remaining"

    const val MILLIS_PER_TICK: Long = 600L

    /**
     * Thirty days of ticks. A `due` beyond that is not a delivery, it is a corrupt or hand-edited
     * row; clamping keeps `clock + remaining` from overflowing `Int` and keeps the parcel alive
     * (arriving late) instead of silently deleting it.
     */
    const val MAX_REMAINING_TICKS: Int = (30L * 24 * 60 * 60 * 1000 / MILLIS_PER_TICK).toInt()

    fun encode(state: DeliveryState, cycle: Int, nowMillis: Long): MutableMap<String, Any> {
        val document = LinkedHashMap<String, Any>(state.lines.size)
        for ((id, lineState) in state.lines) {
            if (lineState.isEmpty) {
                continue
            }
            val entry = LinkedHashMap<String, Any>(2)
            if (lineState.pending.isNotEmpty()) {
                if (lineState.pausedAtSave) entry[PAUSED] = true
                val parcels = ArrayList<Any>(lineState.pending.size)
                for (parcel in lineState.pending) {
                    val remaining = (parcel.dueCycle - cycle).toLong()
                    val encoded =
                        linkedMapOf<String, Any>(
                            PAYLOAD to parcel.payload,
                            AMOUNT to parcel.amount,
                            DUE to nowMillis + remaining * MILLIS_PER_TICK,
                        )
                    if (lineState.pausedAtSave) {
                        encoded[REMAINING] = remaining.coerceIn(0, MAX_REMAINING_TICKS.toLong())
                    }
                    parcels += encoded
                }
                entry[PENDING] = parcels
            }
            if (lineState.stored.isNotEmpty()) {
                val stored = LinkedHashMap<String, Any>(lineState.stored.size)
                for ((payload, amount) in lineState.stored) {
                    stored[payload.toString()] = amount
                }
                entry[STORED] = stored
            }
            document[id] = entry
        }
        return document
    }

    fun decode(document: Map<*, *>, cycle: Int, nowMillis: Long): DeliveryState {
        val state = DeliveryState()
        for ((rawId, rawEntry) in document) {
            val id = rawId as? String ?: continue
            val entry = rawEntry as? Map<*, *> ?: continue
            val lineState = DeliveryLineState()
            lineState.pausedAtSave = entry[PAUSED] == true
            decodePending(entry[PENDING], lineState, cycle, nowMillis)
            decodeStored(entry[STORED], lineState)
            if (lineState.isEmpty) {
                continue
            }
            lineState.recomputeNextDue()
            state.lines[id] = lineState
        }
        state.recomputeNextDue()
        return state
    }

    private fun decodePending(
        raw: Any?,
        lineState: DeliveryLineState,
        cycle: Int,
        nowMillis: Long,
    ) {
        val parcels = raw as? List<*> ?: return
        for (rawParcel in parcels) {
            val parcel = rawParcel as? Map<*, *> ?: continue
            val payload = (parcel[PAYLOAD] as? Number)?.toInt() ?: continue
            val amount = (parcel[AMOUNT] as? Number)?.toInt() ?: continue
            if (amount <= 0) {
                continue
            }
            val pausedRemaining = if (lineState.pausedAtSave) parcel[REMAINING] as? Number else null
            val remaining = if (pausedRemaining != null) {
                pausedRemaining.toLong().coerceIn(0, MAX_REMAINING_TICKS.toLong()).toInt()
            } else {
                val due = (parcel[DUE] as? Number)?.toLong() ?: continue
                remainingTicks(due, nowMillis)
            }
            lineState.pending += PendingDelivery(payload, amount, cycle + remaining)
        }
    }

    private fun decodeStored(raw: Any?, lineState: DeliveryLineState) {
        val stored = raw as? Map<*, *> ?: return
        for ((rawPayload, rawAmount) in stored) {
            val payload =
                when (rawPayload) {
                    is Number -> rawPayload.toInt()
                    is String -> rawPayload.toIntOrNull() ?: continue
                    else -> continue
                }
            val amount = (rawAmount as? Number)?.toInt() ?: continue
            if (amount <= 0) {
                continue
            }
            lineState.stored[payload] = (lineState.stored[payload] ?: 0) + amount
        }
    }

    fun remainingTicks(dueMillis: Long, nowMillis: Long): Int {
        val delta = dueMillis - nowMillis
        if (delta <= 0L) {
            return 0
        }
        val ticks = (delta + MILLIS_PER_TICK - 1) / MILLIS_PER_TICK
        return ticks.coerceAtMost(MAX_REMAINING_TICKS.toLong()).toInt()
    }
}

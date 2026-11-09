package org.rsmod.content.other.grandexchange.market

enum class GeOfferType {
    Buy,
    Sell,
}

data class GeOwner(val worldType: String, val characterId: Int)

/**
 * One Grand Exchange slot. All coins and items an offer holds live here and only here: [escrowCoins]
 * (a buy offer's unspent coins), the unsold part of a sell offer ([remaining]) and the collection
 * box ([collectItems], [collectCoins]). Game thread only.
 */
class GeOffer(
    val owner: GeOwner,
    val slot: Int,
    val type: GeOfferType,
    val obj: Int,
    val price: Long,
    val count: Int,
    val seq: Long,
) {
    var completed: Int = 0
    var gold: Long = 0
    var escrowCoins: Long = 0
    var aborted: Boolean = false
    var collectItems: Int = 0
    var collectCoins: Long = 0

    /** Held out of matching while a placement or collection waits for its character save. */
    var locked: Boolean = false

    val remaining: Int
        get() = count - completed

    val active: Boolean
        get() = !aborted && completed < count

    val finished: Boolean
        get() = !active

    val collectable: Boolean
        get() = collectItems > 0 || collectCoins > 0

    val clearable: Boolean
        get() = finished && !collectable

    override fun toString(): String =
        "GeOffer(owner=$owner, slot=$slot, type=$type, obj=$obj, price=$price, count=$count, " +
            "completed=$completed, gold=$gold, escrowCoins=$escrowCoins, aborted=$aborted, " +
            "collectItems=$collectItems, collectCoins=$collectCoins, locked=$locked)"
}

data class GeHistoryEntry(val type: GeOfferType, val obj: Int, val count: Int, val gold: Long)

class GeAccount(val owner: GeOwner) {
    val slots: Array<GeOffer?> = arrayOfNulls(SLOT_COUNT)
    val versions: LongArray = LongArray(SLOT_COUNT)
    val history: ArrayDeque<GeHistoryEntry> = ArrayDeque()

    fun touch(slot: Int): Long = ++versions[slot]

    fun record(entry: GeHistoryEntry) {
        history.addFirst(entry)
        while (history.size > HISTORY_SIZE) {
            history.removeLast()
        }
    }

    companion object {
        const val SLOT_COUNT = 8
        const val HISTORY_SIZE = 20
    }
}

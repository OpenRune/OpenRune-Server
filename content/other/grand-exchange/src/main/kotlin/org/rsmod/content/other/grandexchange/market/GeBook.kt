package org.rsmod.content.other.grandexchange.market

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class GeMarketRules(
    val wiggleRoomPercent: Double,
    val gradual: Boolean,
    val gradualFillPercent: Double,
)

/**
 * Every Grand Exchange offer the world knows about, online owners or not, plus the rules that
 * move coins and items between them. Game thread only; pure, so it holds no IO.
 */
class GeBook(private val prices: (Int) -> Long?, var rules: GeMarketRules) {
    private val accounts = HashMap<GeOwner, GeAccount>()
    private val activeByObj = HashMap<ObjKey, MutableList<GeOffer>>()

    var nextSeq: Long = 1

    fun account(owner: GeOwner): GeAccount = accounts.getOrPut(owner) { GeAccount(owner) }

    fun accountOrNull(owner: GeOwner): GeAccount? = accounts[owner]

    fun offer(owner: GeOwner, slot: Int): GeOffer? = accounts[owner]?.slots?.get(slot)

    fun activeOffers(): Sequence<GeOffer> = activeByObj.values.asSequence().flatten()

    /** Restores a stored offer as-is. Used when loading the book at startup. */
    fun restore(offer: GeOffer, version: Long) {
        val account = account(offer.owner)
        check(account.slots[offer.slot] == null) { "Slot already restored: $offer" }
        account.slots[offer.slot] = offer
        account.versions[offer.slot] = max(account.versions[offer.slot], version)
        nextSeq = max(nextSeq, offer.seq + 1)
        if (offer.active) {
            index(offer)
        }
    }

    fun place(
        owner: GeOwner,
        slot: Int,
        type: GeOfferType,
        obj: Int,
        price: Long,
        count: Int,
    ): GeOffer {
        require(price > 0 && count > 0) { "Invalid offer: price=$price, count=$count" }
        val account = account(owner)
        check(account.slots[slot] == null) { "Slot $slot is in use for $owner." }
        val offer = GeOffer(owner, slot, type, obj, price, count, nextSeq++)
        if (type == GeOfferType.Buy) {
            offer.escrowCoins = price * count
        }
        account.slots[slot] = offer
        account.touch(slot)
        index(offer)
        return offer
    }

    fun abort(offer: GeOffer): Boolean {
        if (!offer.active) {
            return false
        }
        when (offer.type) {
            GeOfferType.Buy -> {
                offer.collectCoins += offer.escrowCoins
                offer.escrowCoins = 0
            }
            GeOfferType.Sell -> offer.collectItems += offer.remaining
        }
        offer.aborted = true
        unindex(offer)
        touch(offer)
        return true
    }

    fun takeItems(offer: GeOffer, amount: Int) {
        require(amount in 1..offer.collectItems) { "Cannot take $amount items from $offer" }
        offer.collectItems -= amount
        touch(offer)
    }

    fun takeCoins(offer: GeOffer, amount: Long) {
        require(amount in 1..offer.collectCoins) { "Cannot take $amount coins from $offer" }
        offer.collectCoins -= amount
        touch(offer)
    }

    /** Frees the slot once everything is collected, and returns the history entry it left. */
    fun clearIfDone(offer: GeOffer): GeHistoryEntry? {
        if (!offer.clearable) {
            return null
        }
        val account = account(offer.owner)
        if (account.slots[offer.slot] !== offer) {
            return null
        }
        account.slots[offer.slot] = null
        account.touch(offer.slot)
        if (offer.completed <= 0) {
            return null
        }
        val entry = GeHistoryEntry(offer.type, offer.obj, offer.completed, offer.gold)
        account.record(entry)
        return entry
    }

    /**
     * Trades [offer] against other players' resting offers first, then against the simulated
     * market. Returns every offer whose state changed.
     */
    fun match(offer: GeOffer): Set<GeOffer> {
        val changed = LinkedHashSet<GeOffer>()
        if (!offer.active || offer.locked) {
            return changed
        }
        matchPlayers(offer, changed)
        if (offer.active) {
            simulate(offer, changed)
        }
        return changed
    }

    fun simulate(offer: GeOffer, changed: MutableSet<GeOffer> = LinkedHashSet()): Set<GeOffer> {
        if (!offer.active || offer.locked) {
            return changed
        }
        val average = prices(offer.obj) ?: return changed
        if (average <= 0) {
            return changed
        }
        val band = average * rules.wiggleRoomPercent / 100.0
        val price =
            when (offer.type) {
                GeOfferType.Buy -> {
                    if (offer.price < average - band) return changed
                    min(offer.price, average)
                }
                GeOfferType.Sell -> {
                    if (offer.price > average + band) return changed
                    max(offer.price, average)
                }
            }
        val quantity =
            if (rules.gradual) {
                val step = ceil(offer.count * rules.gradualFillPercent / 100.0).toLong()
                min(offer.remaining.toLong(), max(1L, step)).toInt()
            } else {
                offer.remaining
            }
        when (offer.type) {
            GeOfferType.Buy -> fillBuy(offer, quantity, price)
            GeOfferType.Sell -> fillSell(offer, quantity, price)
        }
        changed += offer
        return changed
    }

    private fun matchPlayers(offer: GeOffer, changed: MutableSet<GeOffer>) {
        val resting =
            activeByObj[ObjKey(offer.owner.worldType, offer.obj)]
                ?.asSequence()
                ?.filter { it.type != offer.type && it.owner != offer.owner && !it.locked }
                ?.filter {
                    when (offer.type) {
                        GeOfferType.Buy -> it.price <= offer.price
                        GeOfferType.Sell -> it.price >= offer.price
                    }
                }
                ?.sortedWith(
                    when (offer.type) {
                        GeOfferType.Buy -> compareBy<GeOffer>({ it.price }, { it.seq })
                        GeOfferType.Sell -> compareByDescending<GeOffer> { it.price }
                            .thenBy { it.seq }
                    },
                )
                ?.toList()
                ?: return
        for (other in resting) {
            if (!offer.active) {
                break
            }
            if (!other.active) {
                continue
            }
            val quantity = min(offer.remaining, other.remaining)
            val price = if (other.seq < offer.seq) other.price else offer.price
            val buy = if (offer.type == GeOfferType.Buy) offer else other
            val sell = if (offer.type == GeOfferType.Sell) offer else other
            fillBuy(buy, quantity, price)
            fillSell(sell, quantity, price)
            changed += offer
            changed += other
        }
    }

    private fun fillBuy(offer: GeOffer, quantity: Int, price: Long) {
        check(offer.type == GeOfferType.Buy && quantity in 1..offer.remaining && price <= offer.price)
        offer.completed += quantity
        offer.escrowCoins -= quantity * offer.price
        offer.collectItems += quantity
        offer.collectCoins += quantity * (offer.price - price)
        offer.gold += quantity * price
        check(offer.escrowCoins >= 0) { "Escrow went negative: $offer" }
        if (!offer.active) {
            check(offer.escrowCoins == 0L) { "Completed buy kept escrow: $offer" }
            unindex(offer)
        }
        touch(offer)
    }

    private fun fillSell(offer: GeOffer, quantity: Int, price: Long) {
        check(offer.type == GeOfferType.Sell && quantity in 1..offer.remaining && price >= offer.price)
        offer.completed += quantity
        offer.collectCoins += quantity * price
        offer.gold += quantity * price
        if (!offer.active) {
            unindex(offer)
        }
        touch(offer)
    }

    private fun touch(offer: GeOffer) {
        account(offer.owner).touch(offer.slot)
    }

    private fun index(offer: GeOffer) {
        activeByObj.getOrPut(ObjKey(offer.owner.worldType, offer.obj)) { ArrayList() } += offer
    }

    private fun unindex(offer: GeOffer) {
        val key = ObjKey(offer.owner.worldType, offer.obj)
        val list = activeByObj[key] ?: return
        list.removeIf { it === offer }
        if (list.isEmpty()) {
            activeByObj.remove(key)
        }
    }

    private data class ObjKey(val worldType: String, val obj: Int)
}

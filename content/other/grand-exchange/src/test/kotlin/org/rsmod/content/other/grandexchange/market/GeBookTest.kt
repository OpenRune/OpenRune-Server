package org.rsmod.content.other.grandexchange.market

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeBookTest {
    private val prices = HashMap<Int, Long>()
    private val book = GeBook({ prices[it] }, GeMarketRules(5.0, gradual = false, 10.0))

    private val alice = GeOwner("main", 1)
    private val bob = GeOwner("main", 2)
    private val leaguer = GeOwner("raging_echoes_league", 2)

    @Test
    fun `a buy trades at the resting sell's price and refunds the difference`() {
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 90, count = 10)
        book.match(sell)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 10)
        book.match(buy)

        assertTrue(buy.finished && sell.finished)
        assertEquals(10, buy.collectItems)
        assertEquals(100L, buy.collectCoins)
        assertEquals(900L, buy.gold)
        assertEquals(0L, buy.escrowCoins)
        assertEquals(900L, sell.collectCoins)
        assertEquals(0, sell.collectItems)
    }

    @Test
    fun `a sell placed under a resting buy gets the buy's price`() {
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 120, count = 5)
        book.match(buy)
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 5)
        book.match(sell)

        assertEquals(600L, sell.collectCoins)
        assertEquals(0L, buy.collectCoins)
        assertEquals(5, buy.collectItems)
    }

    @Test
    fun `a partial match leaves the rest of the buy waiting with its escrow`() {
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 4)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 10)
        book.match(buy)

        assertTrue(sell.finished)
        assertTrue(buy.active)
        assertEquals(6, buy.remaining)
        assertEquals(600L, buy.escrowCoins)
        assertEquals(4, buy.collectItems)
    }

    @Test
    fun `cheapest sell fills first, oldest first on a tie`() {
        val dear = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 110, count = 1)
        val cheapOld = book.place(bob, 1, GeOfferType.Sell, SHARK, price = 100, count = 1)
        val cheapNew = book.place(GeOwner("main", 3), 0, GeOfferType.Sell, SHARK, 100, 1)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 120, count = 1)
        book.match(buy)

        assertTrue(cheapOld.finished)
        assertTrue(cheapNew.active)
        assertTrue(dear.active)
    }

    @Test
    fun `own offers never match each other`() {
        val sell = book.place(alice, 0, GeOfferType.Sell, SHARK, price = 100, count = 1)
        val buy = book.place(alice, 1, GeOfferType.Buy, SHARK, price = 100, count = 1)
        book.match(buy)

        assertTrue(sell.active && buy.active)
    }

    @Test
    fun `offers on different world types never match`() {
        val sell = book.place(leaguer, 0, GeOfferType.Sell, SHARK, price = 100, count = 1)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 1)
        book.match(buy)

        assertTrue(sell.active && buy.active)
    }

    @Test
    fun `a locked offer is neither matched nor matched against`() {
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 1)
        sell.locked = true
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 1)
        assertTrue(book.match(buy).isEmpty())
        buy.locked = true
        sell.locked = false
        assertTrue(book.match(sell).isEmpty())
        assertTrue(book.match(buy).isEmpty())
    }

    @Test
    fun `the simulated market fills inside the band at the better of the two prices`() {
        prices[SHARK] = 1000
        val generous = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 1500, count = 2)
        book.match(generous)
        assertTrue(generous.finished)
        assertEquals(2000L, generous.gold)
        assertEquals(1000L, generous.collectCoins)

        val lowball = book.place(alice, 1, GeOfferType.Buy, SHARK, price = 960, count = 1)
        book.match(lowball)
        assertEquals(960L, lowball.gold)

        val cheapSell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 500, count = 1)
        book.match(cheapSell)
        assertEquals(1000L, cheapSell.collectCoins)
    }

    @Test
    fun `offers outside the band wait`() {
        prices[SHARK] = 1000
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 949, count = 1)
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 1051, count = 1)
        assertTrue(book.match(buy).isEmpty())
        assertTrue(book.match(sell).isEmpty())
        assertTrue(buy.active && sell.active)
    }

    @Test
    fun `items without a price only trade between players`() {
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 1000, count = 1)
        assertTrue(book.match(buy).isEmpty())
    }

    @Test
    fun `gradual mode fills a share of the offer each pass`() {
        prices[SHARK] = 100
        book.rules = GeMarketRules(5.0, gradual = true, gradualFillPercent = 30.0)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 10)
        book.match(buy)
        assertEquals(3, buy.completed)
        book.simulate(buy)
        book.simulate(buy)
        book.simulate(buy)
        assertEquals(10, buy.completed)
        assertEquals(0L, buy.escrowCoins)
    }

    @Test
    fun `aborting returns what is left to the collection box once`() {
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 10)
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 3)
        book.match(sell)
        assertTrue(book.abort(buy))
        assertFalse(book.abort(buy))
        assertEquals(700L, buy.collectCoins)
        assertEquals(3, buy.collectItems)
        assertEquals(0L, buy.escrowCoins)

        val unsold = book.place(bob, 1, GeOfferType.Sell, SHARK, price = 100, count = 5)
        book.abort(unsold)
        assertEquals(5, unsold.collectItems)
        assertFalse(book.match(unsold).isNotEmpty())
    }

    @Test
    fun `an aborted offer no longer matches`() {
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 1)
        book.abort(sell)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 1)
        book.match(buy)
        assertTrue(buy.active)
        assertEquals(1, sell.collectItems)
    }

    @Test
    fun `collecting more than the box holds is refused`() {
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 1)
        book.abort(sell)
        assertThrows(IllegalArgumentException::class.java) { book.takeItems(sell, 2) }
        assertThrows(IllegalArgumentException::class.java) { book.takeCoins(sell, 1) }
        book.takeItems(sell, 1)
        assertThrows(IllegalArgumentException::class.java) { book.takeItems(sell, 1) }
    }

    @Test
    fun `a slot frees only once fully collected, and leaves history`() {
        val sell = book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 2)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 2)
        book.match(buy)

        book.takeItems(buy, 1)
        assertNull(book.clearIfDone(buy))
        book.takeItems(buy, 1)
        val entry = book.clearIfDone(buy)
        assertEquals(GeHistoryEntry(GeOfferType.Buy, SHARK, 2, 200), entry)
        assertNull(book.offer(alice, 0))
        assertEquals(listOf(entry), book.account(alice).history.toList())
        assertTrue(book.offer(bob, 0) === sell)
    }

    @Test
    fun `placing into a used slot fails`() {
        book.place(alice, 0, GeOfferType.Buy, SHARK, price = 1, count = 1)
        assertThrows(IllegalStateException::class.java) {
            book.place(alice, 0, GeOfferType.Buy, SHARK, price = 1, count = 1)
        }
    }

    @Test
    fun `every change raises the slot's version`() {
        val account = book.account(alice)
        val buy = book.place(alice, 0, GeOfferType.Buy, SHARK, price = 100, count = 2)
        val placed = account.versions[0]
        book.match(book.place(bob, 0, GeOfferType.Sell, SHARK, price = 100, count = 1))
        val filled = account.versions[0]
        book.abort(buy)
        val aborted = account.versions[0]
        assertTrue(placed < filled && filled < aborted)
    }

    @Test
    fun `random trading never creates or destroys coins or items`() {
        val random = Random(42)
        prices[SHARK] = 1000
        val owners = (1..6).map { GeOwner("main", it) }
        var coinsIn = 0L
        var itemsIn = 0L
        var marketCoins = 0L
        var marketItems = 0L
        repeat(2000) {
            val owner = owners.random(random)
            val account = book.account(owner)
            val slot = random.nextInt(GeAccount.SLOT_COUNT)
            val offer = account.slots[slot]
            if (offer == null) {
                val type = if (random.nextBoolean()) GeOfferType.Buy else GeOfferType.Sell
                val price = random.nextLong(900, 1100)
                val count = random.nextInt(1, 50)
                val placed = book.place(owner, slot, type, SHARK, price, count)
                if (type == GeOfferType.Buy) coinsIn += price * count else itemsIn += count
                val before = snapshot()
                book.match(placed)
                val after = snapshot()
                marketCoins += before.first - after.first
                marketItems += before.second - after.second
            } else if (random.nextInt(4) == 0) {
                book.abort(offer)
            } else if (offer.collectable) {
                if (offer.collectItems > 0) {
                    itemsIn -= offer.collectItems
                    book.takeItems(offer, offer.collectItems)
                }
                if (offer.collectCoins > 0) {
                    coinsIn -= offer.collectCoins
                    book.takeCoins(offer, offer.collectCoins)
                }
                book.clearIfDone(offer)
            }
        }
        val (heldCoins, heldItems) = snapshot()
        assertEquals(coinsIn, heldCoins + marketCoins)
        assertEquals(itemsIn, heldItems + marketItems)
    }

    /** Coins and items the book holds across every slot: escrow, unsold stock and boxes. */
    private fun snapshot(): Pair<Long, Long> {
        var coins = 0L
        var items = 0L
        for (owner in (1..6).map { GeOwner("main", it) }) {
            for (offer in book.account(owner).slots.filterNotNull()) {
                coins += offer.escrowCoins + offer.collectCoins
                items += offer.collectItems
                if (offer.type == GeOfferType.Sell && offer.active) items += offer.remaining
            }
        }
        return coins to items
    }

    private companion object {
        const val SHARK = 385
    }
}

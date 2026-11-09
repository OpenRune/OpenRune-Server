package org.rsmod.content.other.grandexchange

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.grandexchange.GrandExchange.Companion.held
import org.rsmod.content.other.grandexchange.market.GeOfferType
import org.rsmod.content.other.grandexchange.persistence.GePersistence
import org.rsmod.content.other.grandexchange.persistence.GeRow
import org.rsmod.content.other.grandexchange.persistence.GeStoredBook
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(UncheckedType::class)
class GrandExchangeItemFlowTest {
    @Test
    fun GameTestState.`a buy takes exactly its coins and an abort gives every coin back`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(coins, 10_000)

            assertNull(ge.placeBuy(player, 0, shark, count = 10, price = 7))
            assertEquals(10_000 - 70, player.inv.held(coins))
            assertEquals(70L, ge.offer(player, 0).escrowCoins)

            assertNull(ge.abort(player, 0))
            assertNull(ge.collect(player, 0))
            assertEquals(10_000, player.inv.held(coins))
            assertNull(ge.book.offer(ge.owner(player), 0))
        }

    @Test
    fun GameTestState.`a buy without enough coins changes nothing`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(coins, 69)

            assertNotNull(ge.placeBuy(player, 0, shark, count = 10, price = 7))
            assertEquals(69, player.inv.held(coins))
            assertNull(ge.book.offer(ge.owner(player), 0))
        }

    @Test
    fun GameTestState.`a buy worth more than MAX_VALUE coins is refused`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(coins, Int.MAX_VALUE)

            assertNotNull(ge.placeBuy(player, 0, shark, count = 2, price = Int.MAX_VALUE.toLong()))
            assertEquals(Int.MAX_VALUE, player.inv.held(coins))
            assertNull(ge.book.offer(ge.owner(player), 0))
        }

    @Test
    fun GameTestState.`two confirms on one slot in the same tick take coins once`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(coins, 1_000)

            assertNull(ge.placeBuy(player, 0, shark, count = 1, price = 100))
            assertNotNull(ge.placeBuy(player, 0, shark, count = 1, price = 100))
            assertEquals(900, player.inv.held(coins))
        }

    @Test
    fun GameTestState.`a sell takes plain items before notes, and only what is held`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(shark, 1)
            player.inv[1] = InvObj(shark, 1)
            player.inv[2] = InvObj(notedShark, 5)

            assertNotNull(ge.placeSell(player, 0, shark, count = 8, price = 100_000))
            assertEquals(2, player.inv.held(shark))
            assertEquals(5, player.inv.held(notedShark))

            assertNull(ge.placeSell(player, 0, notedShark, count = 4, price = 100_000))
            assertEquals(0, player.inv.held(shark))
            assertEquals(3, player.inv.held(notedShark))
            val offer = ge.offer(player, 0)
            assertEquals(shark, offer.obj)
            assertEquals(4, offer.count)
        }

    @Test
    fun GameTestState.`a sell whose item vanished before confirming is refused`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(shark, 1)
            player.inv[0] = null

            assertNotNull(ge.placeSell(player, 0, shark, count = 1, price = 100_000))
            assertNull(ge.book.offer(ge.owner(player), 0))
        }

    @Test
    fun GameTestState.`players trade with each other and every coin and item is accounted for`() =
        runExchangeTest { ge ->
            val seller = registerPlayer()
            ge.login(seller)
            player.inv[0] = InvObj(coins, 1_000)
            seller.inv[0] = InvObj(notedShark, 10)

            assertNull(ge.placeBuy(player, 0, shark, count = 10, price = 3))
            assertNull(ge.placeSell(seller, 0, notedShark, count = 10, price = 2))
            ge.tick()

            val buy = ge.offer(player, 0)
            val sell = ge.offer(seller, 0)
            assertTrue(buy.finished && sell.finished)

            assertNull(ge.collect(player, 0))
            assertNull(ge.collect(seller, 0))
            assertEquals(10, player.inv.held(notedShark))
            assertEquals(1_000 - 30, player.inv.held(coins))
            assertEquals(30, seller.inv.held(coins))
            assertEquals(0, seller.inv.held(notedShark))
            assertNull(ge.book.offer(ge.owner(player), 0))
            assertNull(ge.book.offer(ge.owner(seller), 0))
        }

    @Test
    fun GameTestState.`collecting into a full inventory keeps the items in the box`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(shark, 1)
            assertNull(ge.placeSell(player, 0, shark, count = 1, price = 100_000))
            ge.abort(player, 0)
            fillWithBeer()

            assertNotNull(ge.collect(player, 0, noted = false))
            assertEquals(1, ge.offer(player, 0).collectItems)
            assertEquals(0, player.inv.held(shark))

            player.inv[5] = null
            assertNull(ge.collect(player, 0, noted = false))
            assertEquals(1, player.inv.held(shark))
            assertNull(ge.book.offer(ge.owner(player), 0))
        }

    @Test
    fun GameTestState.`coins stop at MAX_VALUE and the rest waits in the box`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(coins, 1_000)
            assertNull(ge.placeBuy(player, 0, shark, count = 10, price = 100))
            ge.abort(player, 0)
            player.inv[0] = InvObj(coins, Int.MAX_VALUE - 400)

            assertNull(ge.collect(player, 0))
            assertEquals(Int.MAX_VALUE, player.inv.held(coins))
            assertEquals(600L, ge.offer(player, 0).collectCoins)

            assertNotNull(ge.collect(player, 0))
            assertEquals(600L, ge.offer(player, 0).collectCoins)
        }

    @Test
    fun GameTestState.`collecting twice never pays twice`() =
        runExchangeTest { ge ->
            player.inv[0] = InvObj(coins, 500)
            assertNull(ge.placeBuy(player, 0, shark, count = 5, price = 100))
            ge.abort(player, 0)

            assertNull(ge.collect(player, 0))
            assertNull(ge.collect(player, 0))
            ge.abort(player, 0)
            assertNull(ge.collect(player, 0))
            assertEquals(500, player.inv.held(coins))
        }

    @Test
    fun GameTestState.`an offer stays out of matching until its placement is saved`() =
        runExchangeTest { ge ->
            persistence.autoConfirm = false
            val seller = registerPlayer()
            ge.login(seller)
            player.inv[0] = InvObj(coins, 100)
            seller.inv[0] = InvObj(shark, 1)
            ge.placeBuy(player, 0, shark, count = 1, price = 100)
            ge.placeSell(seller, 0, shark, count = 1, price = 100)

            ge.tick()
            assertTrue(ge.offer(player, 0).active)
            assertTrue(persistence.writes.isEmpty())

            persistence.confirmAll()
            ge.tick()
            assertTrue(ge.offer(player, 0).finished)
            assertEquals(1, persistence.writes.size)
            assertEquals(2, persistence.writes.single().size)
        }

    @Test
    fun GameTestState.`collected bought items arrive as notes when asked`() =
        runExchangeTest { ge ->
            val seller = registerPlayer()
            ge.login(seller)
            player.inv[0] = InvObj(coins, 500)
            seller.inv[0] = InvObj(notedShark, 5)
            ge.placeBuy(player, 0, shark, count = 5, price = 100)
            ge.placeSell(seller, 0, notedShark, count = 5, price = 100)
            ge.tick()

            assertNull(ge.collect(player, 0, coins = false, noted = true))
            assertEquals(5, player.inv.held(notedShark))
            assertEquals(GeOfferType.Buy, ge.book.account(ge.owner(player)).history.first().type)
        }

    private fun GameTestScope.fillWithBeer() {
        for (slot in player.inv.indices) {
            if (player.inv[slot] == null) player.inv[slot] = InvObj(beer, 1)
        }
    }

    private fun GrandExchange.offer(player: Player, slot: Int) =
        checkNotNull(book.offer(owner(player), slot)) { "No offer in slot $slot." }

    private val persistence = FakePersistence()

    private fun GameTestState.runExchangeTest(body: GameTestScope.(GrandExchange) -> Unit) =
        runInjectedGameTest(GrandExchange::class, TestModule(persistence)) { ge ->
            ge.restore(GeStoredBook(emptyList(), emptyMap()))
            player.clearAllInvs()
            ge.login(player)
            body(ge)
        }

    private class TestModule(private val persistence: FakePersistence) : AbstractModule() {
        override fun configure() {
            bind(GePersistence::class.java).toInstance(persistence)
            bind(GrandExchange::class.java).`in`(Scopes.SINGLETON)
        }
    }

    class FakePersistence : GePersistence {
        var autoConfirm = true
        val writes = ArrayList<List<GeRow>>()
        private val pending = ArrayList<(Boolean) -> Unit>()

        override fun load(onLoaded: (GeStoredBook) -> Unit) {}

        override fun write(rows: List<GeRow>) {
            writes += rows
        }

        override fun saveCharacter(player: Player, onSaved: (Boolean) -> Unit) {
            if (autoConfirm) onSaved(true) else pending += onSaved
        }

        fun confirmAll() {
            pending.forEach { it(true) }
            pending.clear()
        }
    }

    private companion object {
        val coins: Int by lazy { "obj.coins".asRSCM(RSCMType.OBJ) }
        val shark: Int by lazy { "obj.shark".asRSCM(RSCMType.OBJ) }
        val notedShark: Int by lazy { checkNotNull(ServerCacheManager.getItem(shark)).certlink }
        val beer: Int by lazy { "obj.beer".asRSCM(RSCMType.OBJ) }
    }
}

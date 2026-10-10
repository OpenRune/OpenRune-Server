package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.raids.toa.raid.ToaRetrieval.retrievalChest
import org.rsmod.content.raids.toa.raid.ToaRetrieval.retrievalLocked
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ToaRetrievalTest {
    @Test
    fun `stacks under the first tier are free`() {
        val f = Fixture(prices = mapOf(SHARK to 99_999))
        f.chest[0] = InvObj(SHARK, 1)
        assertEquals(0, f.fee())
    }

    @Test
    fun `each stack is charged by the tier its total value reaches`() {
        val f = Fixture(prices = mapOf(SHARK to 100_000, LOBSTER to 1_000_000, MANTA to 10_000_000))
        f.chest[0] = InvObj(SHARK, 1)
        assertEquals(1_000, f.fee())
        f.chest[1] = InvObj(LOBSTER, 1)
        assertEquals(11_000, f.fee())
        f.chest[2] = InvObj(MANTA, 1)
        assertEquals(111_000, f.fee())
    }

    @Test
    fun `stack value is price times count`() {
        val f = Fixture(prices = mapOf(SHARK to 1_000))
        f.chest[0] = InvObj(SHARK, 99)
        assertEquals(0, f.fee())
        f.chest[0] = InvObj(SHARK, 100)
        assertEquals(1_000, f.fee())
        f.chest[0] = InvObj(SHARK, Int.MAX_VALUE)
        assertEquals(100_000, f.fee())
    }

    @Test
    fun `fee is capped`() {
        val f = Fixture(prices = mapOf(SHARK to 10_000_000))
        for (slot in f.chest.indices) f.chest[slot] = InvObj(SHARK, 1)
        assertEquals(500_000, f.fee())
    }

    @Test
    fun `ironmen except ultimate pay half`() {
        val prices = mapOf(SHARK to 100_000)
        for ((mode, expected) in
            listOf(
                PlayerGamemode.NORMAL to 1_000,
                PlayerGamemode.IRONMAN to 500,
                PlayerGamemode.ULTIMATE_IRONMAN to 1_000,
                PlayerGamemode.HARDCORE_IRONMAN to 500,
            )) {
            val f = Fixture(gamemode = mode, prices = prices)
            f.chest[0] = InvObj(SHARK, 1)
            assertEquals(expected, f.fee(), "gamemode $mode")
        }
    }

    @Test
    fun `death keeps the three most valuable items and stores every other one`() {
        val f = Fixture(prices = mapOf(MANTA to 500, SHARK to 400, LOBSTER to 300, MONKFISH to 200))
        f.player.inv[0] = InvObj(LOBSTER, 1)
        f.player.worn[3] = InvObj(MANTA, 1)
        f.player.inv[2] = InvObj(SHARK, 1)
        f.give(f.player.inv, MONKFISH, 5)

        assertTrue(f.store(protectItem = false))

        assertEquals(1, f.player.inv.count(MANTA))
        assertEquals(1, f.player.inv.count(SHARK))
        assertEquals(1, f.player.inv.count(LOBSTER))
        assertEquals(5, f.chest.count(MONKFISH))
        assertEquals(0, f.player.worn.count(MANTA))
        f.assertConserved(mapOf(MANTA to 1, SHARK to 1, LOBSTER to 1, MONKFISH to 5))
    }

    @Test
    fun `protect item keeps a fourth item`() {
        val f = Fixture(prices = mapOf(MANTA to 500, SHARK to 400, LOBSTER to 300, MONKFISH to 200))
        f.player.inv[0] = InvObj(MANTA, 1)
        f.player.inv[1] = InvObj(SHARK, 1)
        f.player.inv[2] = InvObj(LOBSTER, 1)
        f.player.inv[3] = InvObj(MONKFISH, 1)

        f.store(protectItem = true)

        assertEquals(1, f.player.inv.count(MONKFISH))
        assertTrue(f.chest.isEmpty())
    }

    @Test
    fun `dying with nothing leaves an empty unlocked chest`() {
        val f = Fixture()
        f.player.retrievalLocked = 1
        assertFalse(f.store(protectItem = false))
        assertTrue(f.chest.isEmpty())
        assertEquals(0, f.player.retrievalLocked)
    }

    @Test
    fun `chest locks only when the stored items cost a fee`() {
        val cheap = Fixture(prices = mapOf(SHARK to 10))
        cheap.player.inv[0] = InvObj(SHARK, 1)
        cheap.player.inv[1] = InvObj(LOBSTER, 1)
        cheap.player.inv[2] = InvObj(MONKFISH, 1)
        cheap.player.inv[3] = InvObj(MANTA, 1)
        cheap.store(protectItem = false)
        assertTrue(cheap.chest.isNotEmpty())
        assertEquals(0, cheap.player.retrievalLocked)

        val rich = Fixture(prices = mapOf(SHARK to 200_000, LOBSTER to 300_000, MONKFISH to 400_000, MANTA to 500_000))
        rich.player.inv[0] = InvObj(SHARK, 1)
        rich.player.inv[1] = InvObj(LOBSTER, 1)
        rich.player.inv[2] = InvObj(MONKFISH, 1)
        rich.player.inv[3] = InvObj(MANTA, 1)
        rich.store(protectItem = false)
        assertEquals(1, rich.player.retrievalLocked)
    }

    @Test
    fun `a second death replaces the earlier chest instead of adding to it`() {
        val f = Fixture(prices = mapOf(MANTA to 500, SHARK to 400, LOBSTER to 300, MONKFISH to 200))
        f.give(f.chest, MONKFISH, 3)
        f.player.inv[0] = InvObj(MANTA, 1)
        f.player.inv[1] = InvObj(SHARK, 1)
        f.player.inv[2] = InvObj(LOBSTER, 1)
        f.player.inv[3] = InvObj(MONKFISH, 1)

        f.store(protectItem = false)

        assertEquals(1, f.chest.count(MONKFISH))
    }

    @Test
    fun `discarding an empty chest changes nothing`() {
        val f = Fixture()
        f.player.retrievalLocked = 1
        ToaRetrieval.discard(f.player)
        assertEquals(1, f.player.retrievalLocked)
    }

    @Test
    fun `taking everything moves each item once`() {
        val f = Fixture()
        f.chest[0] = InvObj(SHARK, 1)
        f.give(f.chest, LOBSTER, 3)
        f.chest[9] = InvObj(CANNONBALL, 500)

        f.unlockOrTakeAll()

        assertTrue(f.chest.isEmpty())
        f.assertConserved(mapOf(SHARK to 1, LOBSTER to 3, CANNONBALL to 500))
        assertEquals(500, f.player.inv.count(CANNONBALL))
    }

    @Test
    fun `taking everything twice in one tick does not duplicate`() {
        val f = Fixture()
        f.give(f.chest, SHARK, 4)

        f.unlockOrTakeAll()
        f.unlockOrTakeAll()

        f.assertConserved(mapOf(SHARK to 4))
        assertEquals(4, f.player.inv.count(SHARK))
    }

    @Test
    fun `a full inventory leaves the chest untouched`() {
        val f = Fixture()
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj(MONKFISH, 1)
        f.chest[0] = InvObj(SHARK, 2)
        f.chest[1] = InvObj(LOBSTER, 1)

        f.unlockOrTakeAll()

        assertEquals(2, f.chest.count(SHARK))
        assertEquals(1, f.chest.count(LOBSTER))
        assertEquals(0, f.player.inv.count(SHARK))
        assertTrue(f.output().contains("Not enough space"))
    }

    @Test
    fun `partial space moves what fits and keeps the remainder in the chest`() {
        val f = Fixture()
        for (slot in 0 until f.player.inv.size - 2) f.player.inv[slot] = InvObj(MONKFISH, 1)
        for (slot in 0 until 4) f.chest[slot] = InvObj(SHARK, 1)

        f.unlockOrTakeAll()

        assertEquals(2, f.player.inv.count(SHARK))
        assertEquals(2, f.chest.count(SHARK))
        f.assertConserved(mapOf(SHARK to 4, MONKFISH to f.player.inv.size - 2))
    }

    @Test
    fun `a stack that would overflow stays in the chest`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, Int.MAX_VALUE)
        f.chest[0] = InvObj(COINS, 5)

        f.unlockOrTakeAll()

        assertEquals(Int.MAX_VALUE, f.player.inv.count(COINS))
        assertEquals(5, f.chest.count(COINS))
    }

    @Test
    fun `unlocking takes the fee from the inventory first and then the bank`() {
        val f = Fixture(prices = mapOf(SHARK to 100_000))
        f.lockedChestWithShark()
        f.player.inv[0] = InvObj(COINS, 400)
        f.bank[0] = InvObj(COINS, 700)

        f.unlockOrTakeAll()

        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(100, f.bank.count(COINS))
        assertEquals(0, f.player.retrievalLocked)
        assertEquals(1, f.chest.count(SHARK))
    }

    @Test
    fun `unlocking from the inventory alone leaves the bank untouched`() {
        val f = Fixture(prices = mapOf(SHARK to 100_000))
        f.lockedChestWithShark()
        f.player.inv[0] = InvObj(COINS, 1_500)
        f.bank[0] = InvObj(COINS, 700)

        f.unlockOrTakeAll()

        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(700, f.bank.count(COINS))
    }

    @Test
    fun `unlocking without enough coins takes nothing`() {
        val f = Fixture(prices = mapOf(SHARK to 100_000))
        f.lockedChestWithShark()
        f.player.inv[0] = InvObj(COINS, 400)
        f.bank[0] = InvObj(COINS, 599)

        f.unlockOrTakeAll()

        assertEquals(400, f.player.inv.count(COINS))
        assertEquals(599, f.bank.count(COINS))
        assertEquals(1, f.player.retrievalLocked)
        assertEquals(1, f.chest.count(SHARK))
    }

    @Test
    fun `a second click after unlocking takes items without charging again`() {
        val f = Fixture(prices = mapOf(SHARK to 100_000))
        f.lockedChestWithShark()
        f.player.inv[0] = InvObj(COINS, 5_000)

        f.unlockOrTakeAll()
        f.unlockOrTakeAll()

        assertEquals(4_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SHARK))
        assertTrue(f.chest.isEmpty())
    }

    @Test
    fun `taking by item is refused while the chest is locked`() {
        val f = Fixture(prices = mapOf(SHARK to 100_000))
        f.lockedChestWithShark()

        f.itemOp(slot = 0, IfButtonOp.Op2)

        assertEquals(1, f.chest.count(SHARK))
        assertEquals(0, f.player.inv.count(SHARK))
    }

    @Test
    fun `taking by item moves every stack of that item and nothing else`() {
        val f = Fixture()
        f.chest[0] = InvObj(SHARK, 1)
        f.chest[2] = InvObj(LOBSTER, 1)
        f.chest[3] = InvObj(LOBSTER, 1)
        f.chest[4] = InvObj(SHARK, 1)
        f.chest[5] = InvObj(SHARK, 1)
        f.chest[6] = InvObj(SHARK, 1)

        f.itemOp(slot = 0, IfButtonOp.Op2)

        assertEquals(4, f.player.inv.count(SHARK))
        assertEquals(0, f.chest.count(SHARK))
        assertEquals(2, f.chest.count(LOBSTER))
        f.assertConserved(mapOf(SHARK to 4, LOBSTER to 2))
    }

    @Test
    fun `taking by item from a slot that emptied does nothing`() {
        val f = Fixture()
        f.chest[1] = InvObj(LOBSTER, 2)

        f.itemOp(slot = 0, IfButtonOp.Op2)

        assertEquals(2, f.chest.count(LOBSTER))
        assertEquals(0, f.player.inv.count(LOBSTER))
    }

    @Test
    fun `taking by item with a full inventory keeps the stacks in the chest`() {
        val f = Fixture()
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj(MONKFISH, 1)
        f.chest[0] = InvObj(SHARK, 1)
        f.chest[1] = InvObj(SHARK, 1)

        f.itemOp(slot = 0, IfButtonOp.Op2)

        assertEquals(2, f.chest.count(SHARK))
        assertEquals(0, f.player.inv.count(SHARK))
    }

    private class Fixture(
        private val gamemode: Int = PlayerGamemode.NORMAL,
        private val prices: Map<String, Int> = emptyMap(),
    ) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("toa-retrieval-test")
        private val context =
            ProtectedAccessContextFactory.empty().copy(getEventBus = { events })
        private val marketPrices =
            object : MarketPrices {
                override fun get(type: ItemServerType): Int? = prices[type.internalName]
            }
        private val script = ToaRetrievalScript(marketPrices)

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 791L
                slotId = 1
                assignUid()
                coords = CoordGrid(3360, 5160, 0)
                currentMapClock = 100
                processedMapClock = 100
                gamemode = this@Fixture.gamemode
                inv = Inventory(inventoryType("inv.inv"), arrayOfNulls(28))
                worn = Inventory(inventoryType("inv.worn"), arrayOfNulls(14))
            }

        val chest: Inventory
            get() = player.retrievalChest

        val bank: Inventory
            get() = player.invMap.getOrPut("inv.bank")

        init {
            chest.fillNulls()
            bank.fillNulls()
        }

        private fun access() = ProtectedAccess(player, coroutine, context)

        fun fee(): Int = ToaRetrieval.fee(player, marketPrices)

        fun store(protectItem: Boolean): Boolean {
            val drops = PlayerDeathDrops(mockk<MapClock>(), mockk(), marketPrices, mockk())
            return ToaRetrieval.store(player, drops, marketPrices, protectItem)
        }

        fun unlockOrTakeAll() = with(script) { access().unlockOrTakeAll() }

        fun itemOp(slot: Int, op: IfButtonOp) = with(script) { access().itemOp(slot, op) }

        fun give(into: Inventory, obj: String, count: Int) {
            var remaining = count
            for (slot in into.indices) {
                if (remaining == 0) return
                if (into[slot] != null) continue
                into[slot] = InvObj(obj, 1)
                remaining--
            }
        }

        fun lockedChestWithShark() {
            chest[0] = InvObj(SHARK, 1)
            player.retrievalLocked = 1
        }

        fun output(): String = client.messages.joinToString("\n")

        fun assertConserved(expected: Map<String, Int>) {
            for ((obj, count) in expected) {
                val total =
                    player.inv.count(obj) +
                        player.worn.count(obj) +
                        chest.count(obj) +
                        bank.count(obj)
                assertEquals(count, total, "total of $obj")
            }
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val SHARK = "obj.shark"
        private const val LOBSTER = "obj.lobster"
        private const val MONKFISH = "obj.monkfish"
        private const val MANTA = "obj.mantaray"
        private const val CANNONBALL = "obj.mcannonball"
        private const val COINS = "obj.coins"

        private val restored = mutableListOf<() -> Unit>()

        private fun inventoryType(name: String) =
            checkNotNull(ServerCacheManager.getInventory(name.asRSCM()))

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}

package org.rsmod.api.player.skilling

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingByproduct
import org.rsmod.api.player.events.skilling.SkillingInput
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SkillingProductAwardTest {
    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `the product replaces its inputs and grants xp`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.Success, result)
        assertEquals(0, f.count(IRON_ORE))
        assertEquals(0, f.count(COAL))
        assertEquals(1, f.count(STEEL_BAR))
        assertEquals(FINE_XP, f.fineXp())
        assertEquals(1, f.completed.size)
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `no room for the product takes nothing and grants nothing`(entry: Entry) {
        val f = Fixture(entry)
        f.give(COINS, 10)
        f.fillWith(IRON_ORE)
        val before = f.snapshot()

        val result = f.award(f.bar(inputs = listOf(SkillingInput(COINS, 1))))

        assertEquals(SkillingAwardResult.InventoryFull, result)
        assertEquals(before, f.snapshot())
        assertEquals(0, f.fineXp())
        assertTrue(f.completed.isEmpty())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a missing input cancels without taking the others`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        val before = f.snapshot()

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.Cancelled, result)
        assertEquals(before, f.snapshot())
        assertEquals(0, f.fineXp())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `too few of an input cancels without taking any`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 1)
        val before = f.snapshot()

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.Cancelled, result)
        assertEquals(before, f.snapshot())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a cancelling subscriber keeps every item and grants nothing`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)
        f.onPrepare { product.cancelled = true }
        val before = f.snapshot()

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.Cancelled, result)
        assertEquals(before, f.snapshot())
        assertEquals(0, f.fineXp())
        assertTrue(f.completed.isEmpty())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a subscriber's changed count is what gets added, for the same inputs`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)
        f.onPrepare { product.count = 2 }

        f.award(f.bar())

        assertEquals(0, f.count(IRON_ORE))
        assertEquals(0, f.count(COAL))
        assertEquals(2, f.count(STEEL_BAR))
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a subscriber's added input is taken with the rest or not at all`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)
        f.onPrepare { product.inputs += SkillingInput(COINS, 5) }
        val before = f.snapshot()

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.Cancelled, result)
        assertEquals(before, f.snapshot())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `outputs sent to the bank take inputs from the inventory`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)
        f.onPrepare { product.destination = f.bank }

        val result = f.award(f.bar(byproducts = listOf(SkillingByproduct(COINS, 3))))

        assertEquals(SkillingAwardResult.Success, result)
        assertEquals(0, f.player.inv.occupiedSpace())
        assertEquals(1, f.count(STEEL_BAR, f.bank))
        assertEquals(3, f.count(COINS, f.bank))
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a full inventory does not block outputs sent to the bank`(entry: Entry) {
        val f = Fixture(entry)
        f.give(COINS, 10)
        f.fillWith(IRON_ORE)
        f.onPrepare { product.destination = f.bank }

        val result = f.award(f.bar(inputs = listOf(SkillingInput(COINS, 1))))

        assertEquals(SkillingAwardResult.Success, result)
        assertEquals(9, f.count(COINS))
        assertEquals(1, f.count(STEEL_BAR, f.bank))
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a full destination keeps the inventory's inputs`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)
        val full = Inventory(InventoryServerType(size = 1), arrayOfNulls(1))
        full[0] = InvObj(COINS.asRSCM(), 1)
        f.onPrepare { product.destination = full }
        val before = f.snapshot()

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.InventoryFull, result)
        assertEquals(before, f.snapshot())
        assertEquals(listOf(COINS.asRSCM() to 1), f.snapshot(full))
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a product stack at the int limit keeps its inputs`(entry: Entry) {
        val f = Fixture(entry)
        f.give(COINS, Int.MAX_VALUE)
        f.give(IRON_ORE, 1)
        val before = f.snapshot()

        val result =
            f.award(f.bar(item = COINS, inputs = listOf(SkillingInput(IRON_ORE, 1))))

        assertEquals(SkillingAwardResult.InventoryFull, result)
        assertEquals(before, f.snapshot())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `no room for a byproduct rolls back the product and inputs`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.fillWith(COAL)
        val before = f.snapshot()

        val result =
            f.award(
                f.bar(
                    inputs = listOf(SkillingInput(IRON_ORE, 1)),
                    byproducts = listOf(SkillingByproduct(COINS, 1)),
                )
            )

        assertEquals(SkillingAwardResult.InventoryFull, result)
        assertEquals(before, f.snapshot())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a repeat award with inputs for one makes one product`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        f.give(COAL, 2)

        val first = f.award(f.bar())
        val second = f.award(f.bar())

        assertEquals(SkillingAwardResult.Success, first)
        assertEquals(SkillingAwardResult.Cancelled, second)
        assertEquals(1, f.count(STEEL_BAR))
        assertEquals(FINE_XP, f.fineXp())
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `virtual storage pays an input by its item name`(entry: Entry) {
        val f = Fixture(entry)
        f.give(IRON_ORE, 1)
        STORAGE.stored = 2

        val result = f.award(f.bar())

        assertEquals(SkillingAwardResult.Success, result)
        assertEquals(0, STORAGE.stored)
        assertEquals(0, f.count(IRON_ORE))
        assertEquals(1, f.count(STEEL_BAR))
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `virtual storage is untouched when the product does not fit`(entry: Entry) {
        val f = Fixture(entry)
        f.give(COINS, Int.MAX_VALUE)
        f.give(IRON_ORE, 1)
        STORAGE.stored = 2
        val before = f.snapshot()

        val result = f.award(f.bar(item = COINS))

        assertEquals(SkillingAwardResult.InventoryFull, result)
        assertEquals(before, f.snapshot())
        assertEquals(2, STORAGE.stored)
    }

    @ParameterizedTest
    @EnumSource(Entry::class)
    fun `a bonus product skips the prepare event`(entry: Entry) {
        val f = Fixture(entry)
        f.onPrepare { product.cancelled = true }

        val result = f.award(f.bar(inputs = emptyList(), isBonus = true))

        assertEquals(SkillingAwardResult.Success, result)
        assertEquals(1, f.count(STEEL_BAR))
    }

    @Test
    fun `consuming inputs for a failed action is all or nothing`() {
        val f = Fixture(Entry.Protected)
        f.give(IRON_ORE, 1)
        f.give(COAL, 1)
        val before = f.snapshot()

        val short = f.access.consumeSkillingInputs(f.bar().inputs)

        assertFalse(short)
        assertEquals(before, f.snapshot())

        f.give(COAL, 1)
        val enough = f.access.consumeSkillingInputs(f.bar().inputs)

        assertTrue(enough)
        assertEquals(0, f.player.inv.occupiedSpace())
        assertTrue(f.completed.isEmpty())
    }

    enum class Entry {
        Direct,
        Protected,
    }

    private class Fixture(private val entry: Entry) {
        val events = EventBus()
        val player =
            Player().apply {
                inv = inventory("inv.inv")
                worn = inventory("inv.worn")
            }
        val bank = inventory("inv.bank")
        val completed = mutableListOf<SkillingActionCompleteEvent>()
        val access =
            ProtectedAccess(
                player,
                GameCoroutine("skilling-product-award-test"),
                ProtectedAccessContextFactory.empty().copy(getEventBus = { events }),
            )

        init {
            STORAGE.stored = 0
            events.subscribeUnbound(SkillingActionCompleteEvent::class.java) { completed += this }
        }

        fun onPrepare(action: SkillingProductPrepareEvent.() -> Unit) {
            events.subscribeUnbound(SkillingProductPrepareEvent::class.java, action)
        }

        fun award(product: SkillingProduct): SkillingAwardResult =
            when (entry) {
                Entry.Direct -> player.awardSkillingProduct(events, product)
                Entry.Protected -> access.awardSkillingProduct(product)
            }

        fun bar(
            item: String = STEEL_BAR,
            inputs: List<SkillingInput> =
                listOf(SkillingInput(IRON_ORE, 1), SkillingInput(COAL, 2)),
            byproducts: List<SkillingByproduct> = emptyList(),
            isBonus: Boolean = false,
        ): SkillingProduct =
            SkillingProduct(
                player = player,
                skill = SMITHING,
                item = item,
                experience = XP,
                source = SkillingProductSource.GodswordHilt,
                isBonus = isBonus,
                inputs = inputs,
                byproducts = byproducts,
            )

        fun give(item: String, count: Int) {
            val obj = InvObj(item.asRSCM(), count)
            if (isStackable(item) || count == 1) {
                player.inv[player.inv.indexOfNull()] = obj
            } else {
                repeat(count) { player.inv[player.inv.indexOfNull()] = InvObj(item.asRSCM(), 1) }
            }
        }

        fun fillWith(item: String) {
            while (player.inv.indexOfNull() != -1) {
                player.inv[player.inv.indexOfNull()] = InvObj(item.asRSCM(), 1)
            }
        }

        fun count(item: String, inv: Inventory = player.inv): Long =
            inv.objs.filterNotNull()
                .filter { it.id == item.asRSCM() }
                .sumOf { it.count.toLong() }

        fun snapshot(inv: Inventory = player.inv): List<Pair<Int, Int>?> =
            inv.objs.map { obj -> obj?.let { it.id to it.count } }

        fun fineXp(): Int = player.statMap.getFineXP(SMITHING)

        private fun isStackable(item: String): Boolean =
            checkNotNull(ServerCacheManager.getItem(item.asRSCM())).stackable

        private fun Inventory.indexOfNull(): Int = objs.indexOfFirst { it == null }

        private fun inventory(name: String): Inventory {
            val type = checkNotNull(ServerCacheManager.getInventory(name.asRSCM()))
            return Inventory(type, arrayOfNulls(type.size))
        }
    }

    private class FakeCoalStorage : PlayerItemStorageHook {
        var stored = 0

        override val consumePolicy = VirtualItemConsumePolicy.StorageFirst

        override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
            ctx.itemInternal == COAL

        override fun contains(ctx: PlayerItemStorageContext): Int = stored

        override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int {
            val removed = minOf(stored, amount)
            stored -= removed
            return removed
        }

        override fun add(ctx: PlayerItemStorageContext, amount: Int): Int = 0
    }

    companion object {
        private const val SMITHING = "stat.smithing"
        private const val IRON_ORE = "obj.iron_ore"
        private const val COAL = "obj.coal"
        private const val STEEL_BAR = "obj.steel_bar"
        private const val COINS = "obj.coins"
        private const val XP = 17.5
        private val FINE_XP = PlayerStatMap.toFineXP(XP).toInt()

        private val STORAGE = FakeCoalStorage()
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(setOf(STORAGE)))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}

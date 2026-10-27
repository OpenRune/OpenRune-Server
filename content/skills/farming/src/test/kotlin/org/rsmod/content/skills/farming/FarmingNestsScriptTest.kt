package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.enums.FarmingWysonEnums.farming_wyson_nest_rewards
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingNestsScriptTest {
    @Test
    fun `seed search replaces its own slot and conserves unrelated items`() {
        val f = Fixture(SEEDS, DAGGER)
        assertEquals(totals(SEEDS to 1, DAGGER to 1), f.totals())
        f.open(0)
        assertEquals(totals(EMPTY to 1, "obj.acorn" to 1, DAGGER to 1), f.totals())
        assertEquals(EMPTY.asRSCM(), f.player.inv[0]?.id)
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `Wyson sweetcorn reward contains six seeds`() {
        val item = checkNotNull(ServerCacheManager.getItem("obj.sweetcorn_seed".asRSCM()))
        val roll = farming_wyson_nest_rewards.filterValuesNotNull().backing.entries
            .takeWhile { it.key != item }.sumOf { it.value }
        val f = Fixture(WYSON, roll = roll)
        assertEquals(totals(WYSON to 1), f.totals())
        f.open(0)
        assertEquals(totals(EMPTY to 1, "obj.sweetcorn_seed" to 6), f.totals())
        assertEquals(EMPTY.asRSCM(), f.player.inv[0]?.id)
    }

    @Test
    fun `rings and all three eggs leave one empty nest and one reward`() {
        for ((nest, reward) in listOf(
            RING to "obj.gold_ring",
            RED to "obj.bird_egg_red",
            "obj.bird_nest_egg_green" to "obj.bird_egg_green",
            "obj.bird_nest_egg_blue" to "obj.bird_egg_blue",
        )) {
            val f = Fixture(nest)
            assertEquals(totals(nest to 1), f.totals())
            f.open(0)
            assertEquals(totals(EMPTY to 1, reward to 1), f.totals())
            assertEquals(EMPTY.asRSCM(), f.player.inv[0]?.id)
        }
    }

    @Test
    fun `full inventory rolls back a nonstackable reward without queueing`() {
        val f = Fixture(RING)
        for (slot in 1 until 28) f.put(slot, DAGGER)
        val before = f.totals()
        assertEquals(totals(RING to 1, DAGGER to 27), before)
        f.open(0)
        assertEquals(before, f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `overflowing a seed stack rolls back but the exact limit succeeds`() {
        for (count in listOf(Int.MAX_VALUE, Int.MAX_VALUE - 1)) {
            val f = Fixture(SEEDS)
            f.put(1, "obj.acorn", count)
            val before = f.totals()
            f.open(0)
            if (count == Int.MAX_VALUE) assertEquals(before, f.totals())
            else assertEquals(totals(EMPTY to 1, "obj.acorn" to Int.MAX_VALUE), f.totals())
            assertTrue(f.player.weakQueueList.isEmpty)
        }
    }

    @Test
    fun `a full inventory can still merge seeds into an existing stack`() {
        val f = Fixture(SEEDS)
        f.put(1, "obj.acorn", 7)
        for (slot in 2 until 28) f.put(slot, DAGGER)
        assertEquals(totals(SEEDS to 1, "obj.acorn" to 7, DAGGER to 26), f.totals())
        f.open(0)
        assertEquals(totals(EMPTY to 1, "obj.acorn" to 8, DAGGER to 26), f.totals())
    }

    @Test
    fun `one click queues mixed nests in lowest slot order and conserves every reward`() {
        val f = Fixture(SEEDS)
        f.put(5, RING)
        f.put(9, RED)
        assertEquals(totals(SEEDS to 1, RING to 1, RED to 1), f.totals())
        f.open(5)
        assertEquals(totals(SEEDS to 1, EMPTY to 1, RED to 1, "obj.gold_ring" to 1), f.totals())
        f.tick()
        assertEquals(SEEDS.asRSCM(), f.player.inv[0]?.id)
        f.tick()
        assertEquals(totals(EMPTY to 2, RED to 1, "obj.gold_ring" to 1, "obj.acorn" to 1), f.totals())
        assertEquals(EMPTY.asRSCM(), f.player.inv[0]?.id)
        f.tick()
        f.tick()
        assertEquals(totals(EMPTY to 3, "obj.gold_ring" to 1, "obj.acorn" to 1, "obj.bird_egg_red" to 1), f.totals())
        assertEquals(EMPTY.asRSCM(), f.player.inv[9]?.id)
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `a vanished queued nest and a stale click cannot award more items`() {
        val f = Fixture(SEEDS, RING, DAGGER)
        val stale = checkNotNull(f.player.inv[0])
        f.open(0)
        f.open(0, stale)
        f.player.inv[1] = null
        val before = f.totals()
        assertEquals(totals(EMPTY to 1, "obj.acorn" to 1, DAGGER to 1), before)
        repeat(6) { f.tick() }
        assertEquals(before, f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `repeated same tick clicks conserve every nest and reward`() {
        val f = Fixture(SEEDS, RING, RED)
        val stale = checkNotNull(f.player.inv[0])
        f.open(0)
        f.open(0, stale)
        f.open(1)
        f.open(2)
        repeat(8) { f.tick() }
        assertEquals(totals(EMPTY to 3, "obj.acorn" to 1, "obj.gold_ring" to 1, "obj.bird_egg_red" to 1), f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    private class Fixture(vararg items: String, roll: Int = 0) {
        val player = Player().apply {
            client = SilentClient()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        private val events = EventBus()
        private val launcher = ProtectedAccessLauncher(
            ProtectedAccessContextFactory(
                random = mockk(), eventBus = events, npcList = mockk(), playerList = mockk(),
                collision = mockk(), areaChecker = mockk(), alignment = TextAlignment(),
                locInteractions = mockk(), npcInteractions = mockk(), playerInteractions = mockk(),
                heldInteractions = mockk(), wornInteractions = mockk(), musicPlayer = mockk(),
                marketPrices = mockk(), instantHitProcessor = mockk(), teleportValidator = mockk(),
                hitModifier = mockk(),
            )
        )
        private val processor = PlayerQueueProcessor(events, launcher)

        init {
            items.forEachIndexed { slot, name -> put(slot, name) }
            val random = object : GameRandom {
                override fun of(maxExclusive: Int): Int = roll
                override fun of(minInclusive: Int, maxInclusive: Int): Int = error("Unexpected ranged roll")
                override fun randomDouble(): Double = error("Unexpected double roll")
            }
            with(FarmingNestsScript(random)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun put(slot: Int, name: String, count: Int = 1) {
            player.inv[slot] = InvObj(name, count)
        }

        fun totals(): Map<Int, Int> {
            val totals = mutableMapOf<Int, Int>()
            for (slot in player.inv.indices) {
                val obj = player.inv[slot] ?: continue
                totals.merge(obj.id, obj.count, Int::plus)
            }
            return totals
        }

        fun open(slot: Int, stale: InvObj? = null) {
            val obj = stale ?: checkNotNull(player.inv[slot])
            val type = checkNotNull(ServerCacheManager.getItem(obj.id))
            assertTrue(launcher.launch(player) {
                check(events.publish(this, HeldObjEvents.Op1(slot, obj, type, player.inv)))
            })
        }

        fun tick() {
            player.currentMapClock++
            processor.process(player)
        }
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val SEEDS = "obj.bird_nest_seeds_jan2019"
        private const val WYSON = "obj.bird_nest_decentseeds_jan2019"
        private const val RING = "obj.bird_nest_ring"
        private const val RED = "obj.bird_nest_egg_red"
        private const val EMPTY = "obj.bird_nest_empty"
        private const val DAGGER = "obj.bronze_dagger"
        private val RESTORED = mutableListOf<() -> Unit>()

        private fun totals(vararg entries: Pair<String, Int>): Map<Int, Int> =
            entries.associate { (name, count) -> name.asRSCM() to count }

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
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

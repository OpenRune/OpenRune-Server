package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.enums.FarmingWysonEnums.farming_wyson_nest_rewards
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingNestsScriptTest {
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
    fun `full inventory rolls back the search without queueing`() {
        val f = Fixture(WYSON)
        for (slot in 1 until 28) f.put(slot, DAGGER)
        val before = f.totals()
        assertEquals(totals(WYSON to 1, DAGGER to 27), before)
        f.open(0)
        assertEquals(before, f.totals())
        assertTrue(f.player.weakQueueList.isEmpty)
    }

    @Test
    fun `a vanished queued nest and a stale click cannot award more items`() {
        val f = Fixture(WYSON, WYSON, DAGGER)
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
        val f = Fixture(WYSON, WYSON, WYSON)
        val stale = checkNotNull(f.player.inv[0])
        f.open(0)
        f.open(0, stale)
        f.open(1)
        f.open(2)
        repeat(8) { f.tick() }
        assertEquals(totals(EMPTY to 3, "obj.acorn" to 3), f.totals())
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

    companion object {
        private const val WYSON = "obj.bird_nest_decentseeds_jan2019"
        private const val EMPTY = "obj.bird_nest_empty"
        private const val DAGGER = "obj.bronze_dagger"
        private val RESTORED = mutableListOf<() -> Unit>()

        private fun totals(vararg entries: Pair<String, Int>): Map<Int, Int> =
            entries.associate { (name, count) -> name.asRSCM() to count }

        @JvmStatic
        @BeforeAll
        fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}

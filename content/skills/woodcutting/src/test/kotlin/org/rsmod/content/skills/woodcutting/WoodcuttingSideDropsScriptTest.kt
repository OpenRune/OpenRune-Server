package org.rsmod.content.skills.woodcutting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.skills.woodcutting.scripts.WoodcuttingSideDropsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class WoodcuttingSideDropsScriptTest {
    @Test
    fun `published type roll boundaries select all five nest categories`() {
        val expected = mapOf(
            0 to "obj.bird_nest_egg_red", 1 to "obj.bird_nest_egg_blue",
            2 to "obj.bird_nest_egg_green", 3 to "obj.bird_nest_ring",
            34 to "obj.bird_nest_ring", 35 to "obj.bird_nest_seeds_jan2019",
            99 to "obj.bird_nest_seeds_jan2019",
        )
        for ((roll, nest) in expected) assertEquals(nest, WoodcuttingSideDropsScript.nestFor(roll))
    }

    @Test
    fun `cape increases chance by ten percent at the exact boundary`() {
        val bare = Fixture(chance = 10)
        bare.publish()
        bare.assertNoDrop()
        for (cape in listOf("obj.skillcape_woodcutting", "obj.skillcape_woodcutting_trimmed")) {
            val worn = Fixture(chance = 10)
            worn.player.worn[0] = InvObj(cape)
            worn.publish()
            worn.assertDrop()
            val miss = Fixture(chance = 11)
            miss.player.worn[0] = InvObj(cape)
            miss.publish()
            miss.assertNoDrop()
        }
        val lastBaseHit = Fixture(chance = 9)
        lastBaseHit.publish()
        lastBaseHit.assertDrop()
    }

    @Test
    fun `redwood and rosewood never roll ordinary nests`() {
        for (logs in listOf("obj.redwood_logs", "obj.rosewood_logs")) {
            val f = Fixture()
            f.publish(logs)
            f.assertNoDrop()
            assertTrue(f.random.bounds.isEmpty())
        }
    }

    @Test
    fun `bonus products and other skills cannot award nests`() {
        val f = Fixture()
        f.publish(bonus = true)
        f.publish(skill = "stat.mining")
        f.assertNoDrop()
        assertTrue(f.random.bounds.isEmpty())
    }

    @Test
    fun `rabbit foot changes the type roll range but not the nest chance`() {
        val f = Fixture(type = 94)
        f.player.worn[0] = InvObj("obj.hunting_strung_rabbit_foot")
        f.publish()
        f.assertDrop("obj.bird_nest_seeds_jan2019")
        assertEquals(listOf(2560, 95, 8), f.random.bounds)
    }

    @Test
    fun `blocked neighbours fall back to the players tile`() {
        val f = Fixture(blocked = true)
        f.publish()
        verify(exactly = 1) {
            f.objects.add("obj.bird_nest_egg_red", f.player.coords, 200, f.player, 1, any())
        }
    }

    private class Fixture(chance: Int = 0, type: Int = 0, blocked: Boolean = false) {
        val random = FixedRandom(chance, type)
        val objects = mockk<ObjRepository>()
        private val collision = CollisionFlagMap()
        val player = Player().apply {
            client = SilentClient()
            coords = CoordGrid(3200, 3200, 0)
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            inv[0] = InvObj("obj.bronze_dagger")
        }
        private val events = EventBus()

        init {
            if (!blocked) {
                for (dx in -1..1) for (dz in -1..1) collision[3200 + dx, 3200 + dz, 0] = 0
            }
            every { objects.add(any<String>(), any(), any(), any(), any(), any()) } returns mockk()
            with(WoodcuttingSideDropsScript(random, objects, collision)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun publish(logs: String = "obj.oak_logs", bonus: Boolean = false, skill: String = "stat.woodcutting") {
            val source = SkillingProductSource.Woodcutting(
                mockk(), checkNotNull(ServerCacheManager.getItem(logs.asRSCM())),
            )
            events.publish(SkillingActionCompleteEvent(player,
                SkillingActionContext.Product(skill, logs, 1, 37.5, source, bonus)))
        }

        fun assertDrop(nest: String = "obj.bird_nest_egg_red") {
            verify(exactly = 1) {
                objects.add(nest, player.coords.translate(-1, -1), 200, player, 1, any())
            }
            assertEquals(InvObj("obj.bronze_dagger"), player.inv[0])
            assertEquals(1, player.inv.count("obj.bronze_dagger"))
        }

        fun assertNoDrop() {
            verify(exactly = 0) { objects.add(any<String>(), any(), any(), any(), any(), any()) }
        }
    }

    private class FixedRandom(private val chance: Int, private val type: Int) : GameRandom {
        val bounds = mutableListOf<Int>()
        override fun of(maxExclusive: Int): Int {
            bounds += maxExclusive
            return when (maxExclusive) {
                2560 -> chance
                95, 100 -> type
                else -> 0
            }
        }
        override fun of(minInclusive: Int, maxInclusive: Int): Int = error("Unexpected ranged roll")
        override fun randomDouble(): Double = error("Unexpected double roll")
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
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}

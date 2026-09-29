package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.access.StandardNpcAccessContext
import org.rsmod.api.npc.events.NpcQueueEvents
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahRespawnIntegrationTest {
    @Test
    fun `successive native deaths each award loot once and respawn a fresh boss in the same instance`() {
        val f = Fixture()
        f.encounter.start()
        val first = f.encounter.boss()

        f.beginDeath(first)
        f.encounter.encounters.completeDeath(first)
        assertTrue(first.isSlotAssigned)
        assertTrue(f.kills.isEmpty())
        assertEquals(0, f.groundCoins())
        f.finishDeath(first)

        assertFalse(first.isSlotAssigned)
        assertEquals(1, f.kills.size)
        assertSame(first, f.kills.single().npc)
        assertSame(f.encounter.player, f.kills.single().hero)
        assertEquals(LAND, f.kills.single().dropCoords)
        assertEquals(100, f.groundCoins())
        f.encounter.encounters.completeDeath(first)

        f.encounter.advance(16)
        assertEquals(0, f.encounter.npcs.count())
        assertEquals(100, f.groundCoins())
        f.encounter.advance(1)

        val second = f.encounter.boss()
        assertNotSame(first, second)
        assertTrue(second.isSlotAssigned)
        assertEquals(500, second.hitpoints)
        assertEquals(0, second.damageContributions.damageBy(f.encounter.player))
        assertNull(second.findHero(f.encounter.players))
        assertSame(f.encounter.session, f.encounter.instances.sessionForPlayer(f.encounter.player))
        assertEquals(100, f.groundCoins())

        f.encounter.encounters.completeDeath(first)
        assertSame(second, f.encounter.boss())
        f.beginDeath(second)
        f.finishDeath(second)

        assertEquals(2, f.kills.size)
        assertEquals(listOf(first, second), f.kills.map { it.npc })
        assertEquals(2, f.kills.map { it.lootTrackerEventId }.distinct().size)
        assertEquals(200, f.groundCoins())
        f.encounter.encounters.completeDeath(second)
        f.encounter.advance(17)

        val third = f.encounter.boss()
        assertNotSame(second, third)
        assertEquals(500, third.hitpoints)
        assertEquals(2, f.kills.size)
        assertEquals(200, f.groundCoins())
        assertSame(f.encounter.session, f.encounter.instances.sessionForPlayer(f.encounter.player))
    }

    @Test
    fun `leaving after native death cancels the respawn without removing awarded ground loot`() {
        val f = Fixture()
        f.encounter.start()
        val boss = f.encounter.boss()
        f.beginDeath(boss)
        f.finishDeath(boss)

        f.encounter.encounters.stop(f.encounter.session.id)
        f.encounter.advance(30)

        assertEquals(0, f.encounter.npcs.count())
        assertEquals(1, f.kills.size)
        assertEquals(100, f.groundCoins())
    }

    @Test
    fun `logout after native death cancels the respawn`() {
        val f = Fixture()
        f.encounter.start()
        val boss = f.encounter.boss()
        f.beginDeath(boss)
        f.finishDeath(boss)

        f.encounter.player.pendingLogout = true
        f.encounter.advance(30)

        assertEquals(0, f.encounter.npcs.count())
        assertEquals(1, f.kills.size)
        assertEquals(100, f.groundCoins())
    }

    @Test
    fun `leaving during the native death animation cancels drops and any replacement spawn`() {
        val f = Fixture()
        f.encounter.start()
        val boss = f.encounter.boss()
        f.beginDeath(boss)
        assertTrue(boss.activeCoroutine?.isSuspended == true)

        f.encounter.encounters.stop(f.encounter.session.id)
        f.encounter.advance(30)
        boss.advanceActiveCoroutine()

        assertFalse(boss.isSlotAssigned)
        assertEquals(0, f.encounter.npcs.count())
        assertTrue(f.kills.isEmpty())
        assertEquals(0, f.groundCoins())
    }

    private class Fixture {
        val encounter = ZulrahEncounterTest.Fixture()
        val kills = mutableListOf<NpcDeathKillContext>()
        private val objects = ObjRepository(encounter.clock, ObjRegistry(encounter.zoneUpdates))
        private val scriptEvents = EventBus()
        private val context = StandardNpcAccessContext(
            getRandom = { DefaultGameRandom(1) },
            getHitModifier = { error("Death must not modify combat hits") },
            getHitProcessor = { error("Death must not process combat hits") },
        )

        init {
            val death = NpcDeath(
                encounter.npcRepo,
                encounter.players,
                objects,
                setOf(NpcDeathDropHook { true }),
                setOf(NpcDeathKillHook {
                    kills += it
                    objects.add("obj.coins", it.dropCoords, 100, it.hero, count = 100)
                }),
            )
            with(ZulrahCombatScript(encounter.encounters, death)) {
                ScriptContext(scriptEvents, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun beginDeath(npc: Npc) {
            npc.recordDamage(encounter.player, npc.hitpoints)
            npc.hitpoints = 0
            npc.currentMapClock = encounter.clock.cycle
            npc.processedMapClock = encounter.clock.cycle
            npc.previousCoords = npc.coords
            npc.launch {
                val access = StandardNpcAccess(npc, this, context)
                val event = NpcQueueEvents.Type(npc, Unit, "queue.death".asRSCM(RSCMType.QUEUE))
                assertTrue(scriptEvents.publish(access, event))
            }
        }

        fun finishDeath(npc: Npc) {
            repeat(30) {
                if (!npc.isSlotAssigned) return
                encounter.advance(1)
                npc.advanceActiveCoroutine()
            }
            error("Native Zulrah death did not finish within 30 ticks")
        }

        fun groundCoins(): Int = objects.findAll(LAND)
            .filter { it.type == "obj.coins".asRSCM(RSCMType.OBJ) }
            .sumOf { it.count }
    }

    companion object {
        private val LAND = CoordGrid(3228, 3261)

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}

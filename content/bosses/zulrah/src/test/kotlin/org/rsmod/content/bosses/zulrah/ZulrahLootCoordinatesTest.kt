package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.PetDropsRow
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahLootCoordinatesTest {
    @Test
    fun `Zulrah pet is not rolled again by the supplemental boss pet hook`() {
        val bosses = ZulrahCombatScript.BOSS_TYPES.map { ZulrahEncounterManager.type(it).id }.toSet()
        assertTrue(PetDropsRow.all().none { it.npc.id in bosses })
    }

    @Test
    fun `native death hook receives land tile without moving the boss from the water`() {
        val fixture = Fixture()

        fixture.death.spawnDrops(fixture.access, LAND)

        val kill = fixture.kills.single()
        assertEquals(LAND, kill.dropCoords)
        assertSame(fixture.npc, kill.npc)
        assertSame(fixture.hero, kill.hero)
        assertTrue(kill.lootTrackerEventId > 0)
        assertEquals(WATER, fixture.npc.coords)
        assertEquals(WATER, fixture.npc.spawnCoords)
    }

    @Test
    fun `ordinary native drops still default to the npc tile`() {
        val fixture = Fixture()

        fixture.death.spawnDrops(fixture.access)

        val kill = fixture.kills.single()
        assertEquals(WATER, kill.dropCoords)
        assertSame(fixture.npc, kill.npc)
        assertSame(fixture.hero, kill.hero)
        assertEquals(WATER, fixture.npc.coords)
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val hero = Player().apply {
            uuid = 1L
            slotId = 1
            coords = LAND
            lootDropDuration = 100
            assignUid()
        }
        val npc = Npc("npc.snakeboss_boss_ranged", WATER).apply {
            recordDamage(hero, 500)
        }
        val kills = mutableListOf<NpcDeathKillContext>()
        private val clock = MapClock(100)
        private val npcs = NpcList()
        private val players = PlayerList().apply { this[hero.slotId] = hero }
        private val repository = NpcRepository(
            clock,
            NpcRegistry(npcs, CollisionFlagMap(), EventBus()),
            npcs,
        )
        val death = NpcDeath(
            repository,
            players,
            ObjRepository(clock, ObjRegistry(ZoneUpdateMap())),
            setOf(NpcDeathDropHook { true }),
            setOf(NpcDeathKillHook { kills += it }),
        )
        val access = StandardNpcAccess(
            npc,
            GameCoroutine(),
            StandardNpcAccessContext(
                getRandom = { DefaultGameRandom(1) },
                getHitModifier = { error("Drop handling must not modify a combat hit") },
                getHitProcessor = { error("Drop handling must not process a combat hit") },
            ),
        )
    }

    companion object {
        private val WATER = CoordGrid(2266, 3073)
        private val LAND = CoordGrid(2268, 3069)

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}

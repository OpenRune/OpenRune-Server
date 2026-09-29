package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSettings
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.instances.region.InstanceAreaResolver
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahInstanceAllocationTest {
    @Test
    fun `two players entering in one tick keep separate complete arenas`() {
        val f = Fixture()
        val firstPlayer = f.player(1)
        val first = f.create(firstPlayer)
        val firstRegion = checkNotNull(f.regions[first.enter])
        firstPlayer.coords = first.enter
        f.manager.finalizeEntry(firstPlayer, first.session, f.clock.cycle)

        // Player input and telejumps run before post-tick zone activity is updated. The second
        // allocation must preserve the first session even while its activity flags are empty.
        assertTrue(f.regions.isEmpty(firstRegion))
        val second = f.create(f.player(2))
        val secondRegion = checkNotNull(f.regions[second.enter])
        assertTrue(f.regionRepo.isValid(firstRegion))
        assertTrue(f.regionRepo.isValid(secondRegion))
        assertNotEquals(firstRegion.uid, secondRegion.uid)
        assertNotEquals(first.enter, second.enter)

        val sourceTiles = listOf(
            ENTRY,
            CoordGrid(2266, 3073),
            CoordGrid(2248, 3056),
            CoordGrid(2295, 3056),
            CoordGrid(2248, 3099),
            CoordGrid(2295, 3099),
        )
        for (source in sourceTiles) {
            val firstTile = checkNotNull(f.manager.resolveCoord(first.session, source))
            val secondTile = checkNotNull(f.manager.resolveCoord(second.session, source))
            assertNotEquals(firstTile, secondTile)
            assertEquals(source, f.regions.normalizeCoords(firstTile))
            assertEquals(source, f.regions.normalizeCoords(secondTile))
        }
    }

    @Test
    fun `cancelled entry releases its region and owner for reuse`() {
        val f = Fixture()
        val player = f.player(1)
        val created = f.create(player)
        val region = checkNotNull(f.regions[created.enter])
        f.manager.cancelPendingEntry(player, f.clock.cycle)
        f.regions.removeInactiveSmallRegions()
        assertFalse(f.regionRepo.isValid(region))
        assertNull(f.manager.sessionForOwner(checkNotNull(player.uuid)))
        assertTrue(f.regionRepo.isValid(checkNotNull(f.regions[f.create(player).enter])))
    }

    @Test
    fun `last player leaving releases private region but keeps server owned arena`() {
        val f = Fixture()
        val serverOwned = checkNotNull(
            f.manager.createServerOwned("public", f.spec, InstanceAccess.Friends, f.clock.cycle),
        )
        val publicEntry = checkNotNull(f.manager.resolveCoord(serverOwned, ENTRY))
        val publicRegion = checkNotNull(f.regions[publicEntry])
        val player = f.player(1)
        val created = f.create(player)
        val privateRegion = checkNotNull(f.regions[created.enter])
        player.coords = created.enter
        f.manager.finalizeEntry(player, created.session, f.clock.cycle)
        assertEquals(EXIT, f.manager.leave(player, created.session, f.clock.cycle))
        f.regions.removeInactiveSmallRegions()
        assertFalse(f.regionRepo.isValid(privateRegion))
        assertTrue(f.regionRepo.isValid(publicRegion))
        assertNull(f.manager.sessionForOwner(checkNotNull(player.uuid)))
        assertEquals(serverOwned, f.manager.sessionForId(serverOwned.id))
    }

    @Test
    fun `failed entry coordinate resolution does not leak a pinned region or owner`() {
        val f = Fixture()
        val player = f.player(1)
        val invalidArea = ARENA.copy(enterCoord = RegionLocal(0, 0, 0, 0, 0))
        val invalidSpec = f.spec.copy(area = invalidArea)
        assertThrows<IllegalStateException> { f.create(player, invalidSpec) }
        val failedRegion = f.smallRegions.single()
        f.regions.removeInactiveSmallRegions()
        assertFalse(f.regionRepo.isValid(failedRegion))
        assertNull(f.manager.sessionForOwner(checkNotNull(player.uuid)))
        assertTrue(f.manager.sessionsForKey("zulrah").isEmpty())
        assertTrue(f.regionRepo.isValid(checkNotNull(f.regions[f.create(player).enter])))
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val clock = MapClock(100)
        private val events = EventBus()
        private val collision = CollisionFlagMap().apply {
            for (x in 2240..2296 step 8) {
                for (z in 3008..3128 step 8) {
                    allocateIfAbsent(x, z, 0)
                }
            }
        }
        private val npcs = NpcList()
        private val npcRegistry = NpcRegistry(npcs, collision, events)
        private val npcRepo = NpcRepository(clock, npcRegistry, npcs)
        private val players = PlayerList()
        private val locZones = LocZoneStorage()
        val smallRegions = RegionListSmall()
        val regions = RegionRegistry(
            smallRegions,
            RegionListLarge(),
            LocRegistryNormal(ZoneUpdateMap(), collision, locZones),
            collision,
            locZones,
            npcRegistry,
            ControllerRegistry(clock, ControllerList()),
            ZonePlayerActivityBitSet(),
        )
        val regionRepo = RegionRepository(regions)
        val manager = InstanceManager(
            regionRepo,
            npcRepo,
            players,
            events,
            InstanceAreaResolver(),
            clock,
            collision,
        )
        val spec = InstanceSettings(destroyWhenEmpty = true).withArea(ARENA, -1)

        fun player(slot: Int): Player = Player().apply {
            uuid = slot.toLong()
            slotId = slot
            assignUid()
            coords = EXIT
            players[slot] = this
        }

        fun create(player: Player, settings: InstanceSpec = spec): InstanceManager.Result.Created =
            manager.create(player, "zulrah", settings, InstanceAccess.Private, clock.cycle)
                as InstanceManager.Result.Created
    }

    companion object {
        private val ENTRY = CoordGrid(2268, 3069)
        private val EXIT = CoordGrid(2212, 3056)
        private val ARENA = ZulrahInstance.ARENA.copy(
            enterCoord = RegionLocal(ENTRY.level, ENTRY.mx, ENTRY.mz, ENTRY.lx, ENTRY.lz),
            exitCoord = EXIT,
        )

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}

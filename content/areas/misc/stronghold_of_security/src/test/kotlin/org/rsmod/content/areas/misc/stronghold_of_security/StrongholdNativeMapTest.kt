package org.rsmod.content.areas.misc.stronghold_of_security

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.NpcList
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.routefinder.loc.LocLayerConstants

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class StrongholdNativeMapTest {
    @Test
    fun `the scripted locs stand where the scripts expect them`() {
        val expected =
            mapOf(
                "loc.sos_dung_ent_open" to listOf(CoordGrid(3081, 3420, 0)),
                "loc.sos_skelly_bag" to listOf(CoordGrid(1860, 5240, 0)),
                "loc.sos_war_portal" to listOf(CoordGrid(1863, 5238, 0)),
                "loc.sos_war_chest" to listOf(CoordGrid(1907, 5222, 0)),
                "loc.sos_war_ladd_down" to listOf(CoordGrid(1902, 5222, 0)),
                "loc.sos_war_ladd_up" to
                    listOf(CoordGrid(1859, 5244, 0), CoordGrid(1913, 5226, 0)),
                "loc.sos_war_chainbottom" to listOf(CoordGrid(1881, 5232, 0)),
                "loc.sos_fam_portal" to listOf(CoordGrid(2039, 5240, 0)),
                "loc.sos_fam_sack" to listOf(CoordGrid(2021, 5215, 0)),
                "loc.sos_fam_ladd_up" to listOf(CoordGrid(2042, 5246, 0)),
                "loc.sos_fam_ladd_down" to listOf(CoordGrid(2026, 5218, 0)),
                "loc.sos_pest_portal" to listOf(CoordGrid(2120, 5258, 0)),
                "loc.sos_pest_chest" to listOf(CoordGrid(2144, 5280, 0)),
                "loc.sos_pest_ladd_up" to listOf(CoordGrid(2123, 5251, 0)),
                "loc.sos_pest_ladd_down" to listOf(CoordGrid(2148, 5284, 0)),
                "loc.sos_pest_rope_up" to listOf(CoordGrid(2150, 5278, 0)),
                "loc.sos_death_portal" to listOf(CoordGrid(2365, 5212, 0)),
                "loc.sos_death_pram" to listOf(CoordGrid(2344, 5214, 0)),
                "loc.sos_death_ladd_up" to listOf(CoordGrid(2358, 5216, 0)),
                "loc.sos_fam_rope_up" to
                    listOf(
                        CoordGrid(2011, 5192, 0),
                        CoordGrid(2017, 5210, 0),
                        CoordGrid(2031, 5189, 0),
                        CoordGrid(2040, 5208, 0),
                    ),
                "loc.sos_death_rope_up" to
                    listOf(CoordGrid(2309, 5240, 0), StrongholdTravel.CradleRoomChain),
            )
        for ((loc, coords) in expected) {
            assertEquals(coords.sortedBy { it.x }, at(loc).sortedBy { it.x }, loc)
        }
    }

    @Test
    fun `the bone chain out of the dungeon stands beside the cradle of life`() {
        val death = StrongholdFloor.Death
        val chain = StrongholdTravel.CradleRoomChain
        assertTrue(chain.chebyshevDistance(at(death.reward).single()) <= 6)
        assertTrue(chain.chebyshevDistance(death.rewardRoom) <= 6)
    }

    @Test
    fun `every arrival tile is open ground`() {
        val arrivals =
            StrongholdFloor.entries.flatMap { listOf(it.start, it.rewardRoom) } +
                listOf(
                    StrongholdTravel.Surface,
                    StrongholdTravel.WarArrival,
                    StrongholdTravel.FamineArrival,
                    StrongholdTravel.PestilenceArrival,
                    StrongholdTravel.DeathArrival,
                )
        val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR
        for (tile in arrivals) {
            assertEquals(0, map.collision[tile.x, tile.z, tile.level] and blocked, "$tile")
        }
    }

    @Test
    fun `reward rooms are next to their chests`() {
        val chests = StrongholdFloor.entries.associateWith { at(it.reward).single() }
        for ((floor, chest) in chests) {
            assertTrue(floor.rewardRoom.chebyshevDistance(chest) <= 2, floor.name)
        }
    }

    @Test
    fun `every doorway has exactly one space between its pair of doors`() {
        for (floor in StrongholdFloor.entries) {
            val doors = at(floor.face).map { it to floor.face } + at(floor.mirror).map { it to floor.mirror }
            assertTrue(doors.size >= 30, "${floor.name} has ${doors.size} door leaves")
            for ((coords, loc) in doors) {
                val info = map.locAt(coords, loc)
                val sides =
                    listOf(true, false).count { onLocSide ->
                        map.locs.standsBetweenDoors(floor, info, onLocSide)
                    }
                assertEquals(1, sides, "${floor.name} $loc at $coords")
            }
        }
    }

    @Test
    fun `doors always come as a face and a mirror side by side`() {
        for (floor in StrongholdFloor.entries) {
            val faces = at(floor.face)
            val mirrors = at(floor.mirror).toSet()
            assertEquals(faces.size, mirrors.size, floor.name)
            for (face in faces) {
                val info = map.locAt(face, floor.face)
                val partners =
                    listOf(
                        DoorTranslations.translateClose(info.coords, info.shape, info.angle),
                        DoorTranslations.translateCloseOpposite(info.coords, info.shape, info.angle),
                    )
                assertEquals(1, partners.count { it in mirrors }, "${floor.name} $face")
            }
        }
    }

    private fun at(loc: String): List<CoordGrid> =
        map.placed.filter { it.first == loc.asRSCM(RSCMType.LOC) && it.second.level == 0 }.map { it.second }

    private class NativeMap {
        val collision = CollisionFlagMap()
        val placed = mutableListOf<Pair<Int, CoordGrid>>()
        val entities = mutableMapOf<Pair<Int, CoordGrid>, LocEntity>()

        private val events = EventBus()
        private val clock = MapClock().apply { cycle = 100 }
        private val updates = ZoneUpdateMap()
        private val zones = LocZoneStorage()
        private val activity = ZonePlayerActivityBitSet()
        private val npcs = NpcRegistry(NpcList(), collision, events)
        private val normal = LocRegistryNormal(updates, collision, zones)
        private val regions =
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                normal,
                collision,
                zones,
                npcs,
                ControllerRegistry(clock, ControllerList()),
                activity,
            )
        private val locRegistry =
            LocRegistry(zones, normal, LocRegistryRegion(updates, collision, zones, regions))
        val locs = LocRepository(clock, locRegistry, regions)

        init {
            val cache = ServerCacheManager.init(240)
            try {
                val squares =
                    listOf(28, 29, 30, 31, 32, 33, 36).flatMap { x -> listOf(80, 81, 82).map { x to it } } +
                        listOf(48 to 53)
                for ((x, z) in squares) {
                    val group = (x shl 8) or z
                    val tileData = cache.data(MAPS, group, 0) ?: continue
                    val locData = cache.data(MAPS, group, 1) ?: continue
                    val tiles = MapTileDecoder.decode(InlineByteBuf(tileData))
                    val spawns = MapLocListDecoder.decode(InlineByteBuf(locData))
                    for (level in 0..1) for (cx in (x * 64)..(x * 64 + 63) step 8) {
                        for (cz in (z * 64)..(z * 64 + 63) step 8) {
                            collision.allocateIfAbsent(cx, cz, level)
                        }
                    }
                    val builder = GameMapBuilder()
                    val square = MapSquareKey(x, z)
                    GameMapDecoder.putMaps(collision, square, tiles)
                    GameMapDecoder.putLocs(builder, collision, square, tiles, spawns)
                    for ((packed, zone) in builder.zoneBuilders) {
                        val base = ZoneKey(packed).toCoords()
                        for (entry in zone.build().byte2IntEntrySet()) {
                            val key = LocZoneKey(entry.byteKey)
                            val entity = LocEntity(entry.intValue)
                            val coords = base.translate(key.x, key.z)
                            placed += entity.id to coords
                            entities[entity.id to coords] = entity
                        }
                    }
                }
            } finally {
                cache.close()
            }
            for (floor in StrongholdFloor.entries) {
                for (loc in listOf(floor.face, floor.mirror)) {
                    val id = loc.asRSCM(RSCMType.LOC)
                    for ((entityId, coords) in placed) {
                        if (entityId != id || coords.level != 0) continue
                        val entity = entities.getValue(entityId to coords)
                        locs.add(LocInfo(LocLayerConstants.of(entity.shape), coords, entity), Int.MAX_VALUE)
                    }
                }
            }
        }

        fun locAt(coords: CoordGrid, loc: String): LocInfo {
            val id = loc.asRSCM(RSCMType.LOC)
            val entity = entities.getValue(id to coords)
            check(entity.shape == LocShape.WallStraight.id) { "$loc at $coords is not a wall" }
            return LocInfo(LocLayerConstants.of(entity.shape), coords, entity)
        }
    }

    companion object {
        private lateinit var map: NativeMap

        @JvmStatic
        @BeforeAll
        fun load() {
            map = NativeMap()
        }

        @JvmStatic
        @AfterAll
        fun unload() {
            map.placed.clear()
        }
    }
}

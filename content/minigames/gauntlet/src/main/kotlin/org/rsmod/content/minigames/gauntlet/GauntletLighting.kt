package org.rsmod.content.minigames.gauntlet

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout
import org.rsmod.content.minigames.gauntlet.layout.GauntletRoom
import org.rsmod.content.minigames.gauntlet.layout.RoomKind
import org.rsmod.content.minigames.gauntlet.layout.RoomTemplates
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Singleton
class GauntletLighting
@Inject
constructor(private val regions: RegionRegistry, private val locRepo: LocRepository) {
    enum class Edge(val dx: Int, val dz: Int) {
        WEST(-1, 0),
        EAST(1, 0),
        SOUTH(0, -1),
        NORTH(0, 1),
    }

    private val lightOf: Map<Int, String> by lazy {
        LIT.mapKeys { it.key.asRSCM(RSCMType.LOC) }
    }

    fun ProtectedAccess.lightEntry(enter: CoordGrid, run: GauntletRun) {
        val layout = run.layout
        val region = regions[enter] ?: return
        lightCorridor(region, layout.startRoom, layout.bossRoom)
        for (index in 0 until GauntletLayout.GRID * GauntletLayout.GRID) {
            vars["varbit.gauntlet_room_${index}_found"] = 0
        }
        markFound(layout.startRoom)
        markFound(layout.bossRoom)
        vars["varbit.gauntlet_current_room_x"] = layout.startRoom.x
        vars["varbit.gauntlet_current_room_z"] = layout.startRoom.z
    }

    fun roomAt(region: Region, run: GauntletRun, coords: CoordGrid): GauntletRoom? {
        val x = (coords.x - region.southWest.x) / ROOM_TILES
        val z = (coords.z - region.southWest.z) / ROOM_TILES
        if (x !in 0 until GauntletLayout.GRID || z !in 0 until GauntletLayout.GRID) return null
        return run.layout[x, z]
    }

    fun edgeOf(region: Region, coords: CoordGrid): Edge {
        val lx = (coords.x - region.southWest.x) % ROOM_TILES
        val lz = (coords.z - region.southWest.z) % ROOM_TILES
        return listOf(
                lx to Edge.WEST,
                ROOM_TILES - 1 - lx to Edge.EAST,
                lz to Edge.SOUTH,
                ROOM_TILES - 1 - lz to Edge.NORTH,
            )
            .minBy { it.first }
            .second
    }

    fun neighbour(run: GauntletRun, room: GauntletRoom, edge: Edge): GauntletRoom? {
        val x = room.x + edge.dx
        val z = room.z + edge.dz
        if (x !in 0 until GauntletLayout.GRID || z !in 0 until GauntletLayout.GRID) return null
        return run.layout[x, z]
    }

    fun ProtectedAccess.light(
        coords: CoordGrid,
        run: GauntletRun,
        from: GauntletRoom,
        target: GauntletRoom,
    ) {
        val region = regions[coords] ?: return
        reveal(region, target, run.mode)
        run.revealed += target.index
        markFound(target)
        lightCorridor(region, from, target)
        for (edge in Edge.entries) {
            val next = neighbour(run, target, edge) ?: continue
            val litSide = next.index in run.revealed && next.kind != RoomKind.BOSS
            if (litSide && next.index != from.index) lightCorridor(region, target, next)
        }
        player.regionRebuildPending = true
    }

    fun lightBossRoom(coords: CoordGrid, run: GauntletRun) {
        val region = regions[coords] ?: return
        val boss = run.layout.bossRoom
        for (edge in Edge.entries) {
            val next = neighbour(run, boss, edge) ?: continue
            if (next.index in run.revealed) lightStrip(region, boss, edge)
        }
    }

    private fun reveal(region: Region, room: GauntletRoom, mode: GauntletMode) {
        val zoneX = RoomTemplates.zoneX(mode, room.kind)
        val zoneZ = RoomTemplates.zoneZ(room)
        val southWest = region.southWestZone
        val zones = GauntletZones.ROOM_ZONES
        for (level in 0..3) {
            for (x in 0 until zones) {
                for (z in 0 until zones) {
                    val (rx, rz) = RegionRotations.translateZone(room.rotation, x, z, zones, zones)
                    val source = ZoneKey(zoneX + x, zoneZ + z, level)
                    val dest =
                        ZoneKey(
                            southWest.x + room.x * zones + rx,
                            southWest.z + room.z * zones + rz,
                            level,
                        )
                    regions.registerZone(region, dest, RegionZoneCopy(source, room.rotation, null))
                }
            }
        }
    }

    private fun lightCorridor(region: Region, a: GauntletRoom, b: GauntletRoom) {
        val toB = Edge.entries.first { a.x + it.dx == b.x && a.z + it.dz == b.z }
        val toA = Edge.entries.first { b.x + it.dx == a.x && b.z + it.dz == a.z }
        lightStrip(region, a, toB)
        lightStrip(region, b, toA)
    }

    private fun lightStrip(region: Region, room: GauntletRoom, edge: Edge) {
        val originX = region.southWest.x + room.x * ROOM_TILES
        val originZ = region.southWest.z + room.z * ROOM_TILES
        val last = ROOM_TILES - 1
        for (along in 0..last) {
            for (depth in 0..1) {
                val x =
                    when (edge) {
                        Edge.WEST -> originX + depth
                        Edge.EAST -> originX + last - depth
                        else -> originX + along
                    }
                val z =
                    when (edge) {
                        Edge.SOUTH -> originZ + depth
                        Edge.NORTH -> originZ + last - depth
                        else -> originZ + along
                    }
                val coords = CoordGrid(x, z, GauntletZones.WALKABLE_LEVEL)
                for (loc in locRepo.findAll(coords).toList()) {
                    val lit = lightOf[loc.id] ?: continue
                    locRepo.add(coords, lit, Int.MAX_VALUE, loc.angle, loc.shape)
                }
            }
        }
    }

    private fun ProtectedAccess.markFound(room: GauntletRoom) {
        vars["varbit.gauntlet_room_${room.index}_found"] = 1
    }

    companion object {
        const val ROOM_TILES = 16

        private val LIT =
            mapOf(
                "loc.prif_gauntlet_door_wall_unlit_01" to "loc.prif_gauntlet_door_wall_lit_01",
                "loc.prif_gauntlet_door_wall_unlit_02" to "loc.prif_gauntlet_door_wall_lit_02",
                "loc.prif_gauntlet_door_wall_unlit_01_hm" to
                    "loc.prif_gauntlet_door_wall_lit_01_hm",
                "loc.prif_gauntlet_door_wall_unlit_02_hm" to
                    "loc.prif_gauntlet_door_wall_lit_02_hm",
                "loc.prif_gauntlet_wall_arch_blue_unlit" to "loc.prif_gauntlet_wall_arch_blue_lit",
                "loc.prif_gauntlet_wall_arch_red_unlit_hm" to
                    "loc.prif_gauntlet_wall_arch_red_lit_hm",
            )
    }
}

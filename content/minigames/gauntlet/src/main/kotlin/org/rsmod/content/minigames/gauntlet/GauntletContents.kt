package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.minigames.gauntlet.layout.GauntletRoom
import org.rsmod.content.minigames.gauntlet.layout.ResourceKind
import org.rsmod.content.minigames.gauntlet.layout.RoomSlots
import org.rsmod.content.minigames.gauntlet.layout.Tile
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class GauntletContents
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val manager: InstanceManager,
    private val collision: CollisionFlagMap,
) {
    fun spawn(region: Region, run: GauntletRun, room: GauntletRoom, player: Player) {
        val contents = run.contents[room.index] ?: return
        val suffix = if (run.mode.corrupted) "_hm" else ""
        val instanceId = manager.sessionForPlayer(player)?.id
        val taken = hashSetOf<CoordGrid>()
        for (resource in contents.resources) {
            val size = if (resource.kind == ResourceKind.FISHING) FISHING_SIZE else 1
            val coords = placeable(region, room, resource.tile, size, taken) ?: continue
            val loc = "loc.gauntlet_${resource.kind.locName}$suffix"
            locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle[Random.nextInt(4)], SHAPE)
            run.charges[coords] = CHARGES.getValue(resource.kind)
        }
        for (monster in contents.monsters) {
            val coords = placeable(region, room, monster.tile, 1, taken) ?: continue
            val npc = Npc("npc.crystal_${monster.kind.locName}$suffix", coords)
            npcRepo.add(npc, Int.MAX_VALUE)
            if (instanceId != null) manager.attachNpc(instanceId, npc)
        }
    }

    private fun placeable(
        region: Region,
        room: GauntletRoom,
        tile: Tile,
        size: Int,
        taken: MutableSet<CoordGrid>,
    ): CoordGrid? {
        val last = GauntletLighting.ROOM_TILES - size
        for (radius in 0..SEARCH_RADIUS) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (maxOf(abs(dx), abs(dz)) != radius) continue
                    val x = tile.x + dx
                    val z = tile.z + dz
                    if (x !in 0..last || z !in 0..last) continue
                    val coords = coordsOf(region, room, Tile(x, z), size)
                    if (!isFree(coords, size, taken)) continue
                    for (ox in 0 until size) {
                        for (oz in 0 until size) taken += coords.translate(ox, oz)
                    }
                    return coords
                }
            }
        }
        return null
    }

    private fun isFree(coords: CoordGrid, size: Int, taken: Set<CoordGrid>): Boolean {
        for (ox in 0 until size) {
            for (oz in 0 until size) {
                val tile = coords.translate(ox, oz)
                if (tile in taken) return false
                if (collision[tile.x, tile.z, tile.level] and BLOCKED_FLAGS != 0) return false
            }
        }
        return true
    }

    private fun coordsOf(region: Region, room: GauntletRoom, tile: Tile, size: Int): CoordGrid {
        val a = rotate(room, tile.x, tile.z)
        val b = rotate(room, tile.x + size - 1, tile.z + size - 1)
        return CoordGrid(
            region.southWest.x + room.x * GauntletLighting.ROOM_TILES + min(a.x, b.x),
            region.southWest.z + room.z * GauntletLighting.ROOM_TILES + min(a.z, b.z),
            GauntletZones.WALKABLE_LEVEL,
        )
    }

    private fun rotate(room: GauntletRoom, x: Int, z: Int) =
        RegionRotations.translateZone(
            room.rotation,
            x,
            z,
            GauntletLighting.ROOM_TILES,
            GauntletLighting.ROOM_TILES,
        )

    companion object {
        private const val FISHING_SIZE = 2
        private const val SEARCH_RADIUS = GauntletLighting.ROOM_TILES - 1
        private const val BLOCKED_FLAGS =
            CollisionFlag.LOC or CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR
        private val SHAPE = LocShape.CentrepieceStraight
        private val CHARGES =
            mapOf(
                ResourceKind.DEPOSIT to 3,
                ResourceKind.PHREN to 3,
                ResourceKind.LINUM to 3,
                ResourceKind.GRYM to 1,
                ResourceKind.FISHING to 4,
            )

        val SLOTS: RoomSlots by lazy {
            val stream =
                requireNotNull(GauntletContents::class.java.getResourceAsStream(SLOTS_RESOURCE))
            RoomSlots.parse(stream.bufferedReader().readLines())
        }

        private const val SLOTS_RESOURCE = "/gauntlet/room-slots.txt"
    }
}

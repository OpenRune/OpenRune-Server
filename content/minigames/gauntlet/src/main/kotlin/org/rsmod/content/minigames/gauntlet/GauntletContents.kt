package org.rsmod.content.minigames.gauntlet

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.startEncounter
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.minigames.gauntlet.hunllef.HunllefSpecs
import org.rsmod.content.minigames.gauntlet.layout.GauntletRoom
import org.rsmod.api.table.GauntletRoomSlotsRow
import org.rsmod.content.minigames.gauntlet.layout.MonsterKind
import org.rsmod.content.minigames.gauntlet.layout.ResourceKind
import org.rsmod.content.minigames.gauntlet.layout.RoomKind
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
    private val bossDeps: BossDeps,
    private val hunllefSpecs: HunllefSpecs,
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
            val type = "npc.crystal_${monster.kind.locName}$suffix"
            val size = requireNotNull(ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC))).size
            val coords = placeable(region, room, monster.tile, size, taken) ?: continue
            val npc = Npc(type, coords)
            if (monster.kind in MonsterKind.DEMI) npc.apRangeOverride = DEMI_AP_RANGE
            npcRepo.add(npc, NPC_LIFETIME)
            if (instanceId != null) manager.attachNpc(instanceId, npc)
        }
    }

    fun spawnHunllef(region: Region, run: GauntletRun, player: Player) {
        val type = if (run.mode.corrupted) "npc.crystal_hunllef_melee_hm" else "npc.crystal_hunllef_melee"
        val size = requireNotNull(ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC))).size
        val npc = Npc(type, coordsOf(region, run.layout.bossRoom, HUNLLEF_TILE, size))
        npcRepo.add(npc, NPC_LIFETIME)
        manager.sessionForPlayer(player)?.let { manager.attachNpc(it.id, npc) }
        npc.apRangeOverride = HUNLLEF_AP_RANGE
        bossDeps.startEncounter(npc, hunllefSpecs.of(run.mode.corrupted))
        npc.setHeadIcon(HunllefSpecs.HEAD_ICON_SLOT, HunllefSpecs.HEAD_ICON_GRAPHIC, 0)
        npc.anim("seq.hunllef_spawn", delay = HUNLLEF_SPAWN_DELAY)
        run.hunllef = npc
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
        private const val NPC_LIFETIME = 100_000
        private const val FISHING_SIZE = 2
        private const val DEMI_AP_RANGE = 10
        private const val HUNLLEF_SPAWN_DELAY = 120
        private const val HUNLLEF_AP_RANGE = 16
        private val HUNLLEF_TILE = Tile(6, 6)
        private const val SEARCH_RADIUS = GauntletLighting.ROOM_TILES - 1
        private const val WALL_FLAGS =
            CollisionFlag.WALL_NORTH_WEST or
                CollisionFlag.WALL_NORTH or
                CollisionFlag.WALL_NORTH_EAST or
                CollisionFlag.WALL_EAST or
                CollisionFlag.WALL_SOUTH_EAST or
                CollisionFlag.WALL_SOUTH or
                CollisionFlag.WALL_SOUTH_WEST or
                CollisionFlag.WALL_WEST
        private const val BLOCKED_FLAGS =
            CollisionFlag.LOC or
                CollisionFlag.BLOCK_WALK or
                CollisionFlag.GROUND_DECOR or
                WALL_FLAGS
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
            RoomSlots.fromTemplates(
                GauntletRoomSlotsRow.all().map { row ->
                    val tiles =
                        mapOf(
                            ResourceKind.DEPOSIT to row.rock.map(::unpack),
                            ResourceKind.PHREN to row.tree.map(::unpack),
                            ResourceKind.FISHING to row.pond.map(::unpack),
                            ResourceKind.GRYM to row.herb.map(::unpack),
                            ResourceKind.LINUM to row.fibre.map(::unpack),
                        )
                    Triple(RoomKind.entries[row.kind], row.variant, tiles)
                }
            )
        }

        private fun unpack(packed: Int) = Tile(packed shr 8, packed and 0xFF)
    }
}

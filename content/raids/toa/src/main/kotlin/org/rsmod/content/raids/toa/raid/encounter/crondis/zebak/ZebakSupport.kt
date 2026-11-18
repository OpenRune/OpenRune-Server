package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.min
import org.rsmod.api.config.Constants
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

internal val NOOP_NPC_MODIFIER = NpcHitModifier {}

internal fun WorldRepository.projectile(
    spot: String,
    from: CoordGrid,
    to: CoordGrid,
    type: String,
) {
    projAnim(ProjAnim.fromBoundsToCoord(Bounds(from), to, spot.asRSCM(RSCMType.SPOTANIM), type))
}

internal fun WorldRepository.projectile(spot: String, from: CoordGrid, to: Player, type: String) {
    projAnim(ProjAnim.fromBoundsToPlayer(Bounds(from), to, spot.asRSCM(RSCMType.SPOTANIM), type))
}

internal fun WorldRepository.lob(spot: String, from: CoordGrid, to: CoordGrid): Int {
    val tiles = min(from.chebyshevDistance(to), LOB_MAX_TILES)
    projectile(spot, from, to, if (tiles < LOB_MAX_TILES) ZebakProjs.LOB else ZebakProjs.LOB_FAR)
    return 1 + tiles
}

private const val LOB_MAX_TILES = 4

internal class ZebakLandings {
    private val byCycle = HashMap<Int, MutableList<() -> Unit>>()

    fun at(cycle: Int, action: () -> Unit) {
        byCycle.getOrPut(cycle) { ArrayList() } += action
    }

    fun take(cycle: Int): List<() -> Unit> = byCycle.remove(cycle).orEmpty()

    fun clear() {
        byCycle.clear()
    }
}

/**
 * Whether [player] is melee-adjacent to this npc: any of the tiles ringing its footprint, corners
 * included (all eight surrounding tiles for a 1x1, the full outer ring for larger bodies). A corner
 * is not safe from a melee auto. The footprint itself is not "beside".
 */
internal fun Npc.isBeside(player: Player): Boolean = isBeside(coords, size, player.coords)

/** Pure form of [isBeside] on raw coordinates, so the adjacency rule can be tested directly. */
internal fun isBeside(npc: CoordGrid, size: Int, target: CoordGrid): Boolean {
    if (target.level != npc.level) return false
    val maxX = npc.x + size - 1
    val maxZ = npc.z + size - 1
    val inRing = target.x in npc.x - 1..maxX + 1 && target.z in npc.z - 1..maxZ + 1
    val inFootprint = target.x in npc.x..maxX && target.z in npc.z..maxZ
    return inRing && !inFootprint
}

internal fun Npc.besideTile(player: Player): CoordGrid {
    val c = player.coords
    val tiles =
        (0 until size).flatMap { i ->
            listOf(
                CoordGrid(c.x - size, c.z - i, c.level),
                CoordGrid(c.x + 1, c.z - i, c.level),
                CoordGrid(c.x - i, c.z - size, c.level),
                CoordGrid(c.x - i, c.z + 1, c.level),
            )
        }
    return tiles.minBy { it.chebyshevDistance(coords) }
}

internal fun faceAngle(dx: Int, dz: Int): Int =
    when {
        dx == 0 && dz > 0 -> Constants.em_face_north
        dx > 0 && dz > 0 -> Constants.em_face_northeast
        dx > 0 && dz == 0 -> Constants.em_face_east
        dx > 0 -> Constants.em_face_southeast
        dx == 0 -> Constants.em_face_south
        dz < 0 -> Constants.em_face_southwest
        dz == 0 -> Constants.em_face_west
        else -> Constants.em_face_northwest
    }

package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.min
import org.rsmod.api.config.Constants
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

internal val NOOP_NPC_MODIFIER = NpcHitModifier {}

internal fun npcType(name: String): NpcServerType =
    ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!

internal fun spotanim(name: String): SpotanimType = SpotanimType(name.asRSCM(RSCMType.SPOTANIM))

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

internal fun Npc.isBeside(player: Player): Boolean {
    val c = player.coords
    if (c.level != coords.level) return false
    val maxX = coords.x + size - 1
    val maxZ = coords.z + size - 1
    return (c.x in coords.x..maxX && (c.z == coords.z - 1 || c.z == maxZ + 1)) ||
        (c.z in coords.z..maxZ && (c.x == coords.x - 1 || c.x == maxX + 1))
}

internal fun Player.hitTypeless(damage: Int) {
    queueHit(delay = 1, type = HitType.Typeless, damage = damage, modifier = NoopPlayerHitModifier)
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

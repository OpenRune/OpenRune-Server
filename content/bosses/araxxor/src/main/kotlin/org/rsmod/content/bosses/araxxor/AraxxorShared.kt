package org.rsmod.content.bosses.araxxor

import kotlin.math.abs
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal const val ARAXXOR = "npc.araxxor"
internal const val CORPSE_NPC = "npc.araxxor_dead"
internal const val BALL_NPC = "npc.araxxor_acid_cannon_projectile"

internal const val EGG_ACIDIC = "npc.araxxor_minion_egg_venom"
internal const val EGG_MIRRORBACK = "npc.araxxor_minion_egg_mirrorback"
internal const val EGG_RUPTURA = "npc.araxxor_minion_egg_explode"
internal const val MINION_ACIDIC = "npc.araxxor_minion_venom"
internal const val MINION_MIRRORBACK = "npc.araxxor_minion_mirrorback"
internal const val MINION_RUPTURA = "npc.araxxor_minion_explode"

internal const val PATTERN_VARN = "varn.araxxor_pattern"
internal const val HATCH_IN_VARN = "varn.araxxor_hatch_in"
internal const val SPAWN_TICK_VARN = "varn.araxxor_spawn_tick"
internal const val ENGAGE_AT_VARN = "varn.araxxor_engage_at"

internal const val INSTANCE_KEY = "araxxor"

internal const val ACID_POOL_LOC = "loc.araxxor_acidpool"
internal const val POOLS_PROJ = "spotanim.araxxor_pools_proj"
internal const val POOLS_SPLASH = "spotanim.araxxor_pools_splash"

internal const val EGG_SPAWN_ANIM = "seq.egg_araxyte_spawn_01"
internal const val EGG_DEATH_ANIM = "seq.egg_araxyte_death_01"
internal const val HATCH_ANIM = "seq.npc_araxyte02_hatch"
internal const val EXPLODE_ANIM = "seq.npc_araxyte_explode"

internal const val FIRST_HATCH_STANDARDS = 3
internal const val HATCH_INTERVAL_STANDARDS = 6
internal const val EGG_HITPOINTS = 65
internal const val MINION_HITPOINTS = 58

internal const val BOSS_CENTRE_OFFSET = 3
internal const val SMALL_CENTRE_OFFSET = 1

internal val SPAWN_TILE = CoordGrid(3631, 9814, 0)

/**
 * The cleave's 1x3 line lies across the blow: a target to the east or west gets a north-south
 * line. Ties go east-west; this matched 77 of 82 captured cleaves.
 */
internal fun cleaveRunsNorthSouth(boss: CoordGrid, aim: CoordGrid): Boolean {
    val dx = aim.x - (boss.x + BOSS_CENTRE_OFFSET)
    val dz = aim.z - (boss.z + BOSS_CENTRE_OFFSET)
    return abs(dx) > abs(dz)
}

/**
 * The ball rolls along one of eight directions from Araxxor's middle towards the target. An axis is
 * dropped when it is under 40% of the other; captured offsets of (1,-4) and (-1,4) rolled
 * straight and (3,-4) rolled diagonally.
 */
internal fun ballDirection(centre: CoordGrid, aim: CoordGrid): Pair<Int, Int> {
    val dx = aim.x - centre.x
    val dz = aim.z - centre.z
    val ax = abs(dx)
    val az = abs(dz)
    val stepX = if (ax * 5 < az * 2) 0 else Integer.signum(dx)
    val stepZ = if (az * 5 < ax * 2) 0 else Integer.signum(dz)
    return if (stepX == 0 && stepZ == 0) 0 to -1 else stepX to stepZ
}

internal fun axisGap(a: Int, aSize: Int, b: Int, bSize: Int): Int =
    maxOf(a - (b + bSize - 1), b - (a + aSize - 1), 0)

internal fun Npc.edgeDistanceTo(coord: CoordGrid): Int =
    maxOf(axisGap(coords.x, visType.size, coord.x, 1), axisGap(coords.z, visType.size, coord.z, 1))

internal fun Npc.edgeDistanceTo(other: Npc): Int =
    maxOf(
        axisGap(coords.x, visType.size, other.coords.x, other.visType.size),
        axisGap(coords.z, visType.size, other.coords.z, other.visType.size),
    )

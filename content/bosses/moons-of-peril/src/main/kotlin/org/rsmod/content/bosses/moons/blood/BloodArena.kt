package org.rsmod.content.bosses.moons.blood

import kotlin.math.abs
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.area
import org.rsmod.api.bosses.dsl.customTiles
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.locAnim
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersOn
import org.rsmod.api.bosses.dsl.removeLoc
import org.rsmod.api.bosses.dsl.spawnLoc
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.bosses.spec.TileSet
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals

internal object BloodArena {
    val AREA =
        area(
            spawnTile(Moon.ARENA_WEST, Moon.ARENA_SOUTH),
            spawnTile(Moon.ARENA_EAST, Moon.ARENA_NORTH),
        )

    const val HIDE_NPC = "varbit.pmoon_boss_hide_npc"
    const val HEAL_SYNTH = "synth.varl_pm_blood_heal_04"
    const val AD_HP = "varbit.pmoon_boss_ad_hp"
    const val POOL_LOC = "loc.pmoon_boss_blood_pool"
    const val POOL_IDLE = "seq.vfx_djinn_blood_pool_idle_01"
    const val POOL_LIFETIME = 5

    fun perimeter(min: Int, max: Int): List<Pair<Int, Int>> =
        (min..max)
            .flatMap { x -> (min..max).map { z -> x to z } }
            .filter { (x, z) -> x == min || x == max || z == min || z == max }

    fun ringAroundPlayer(): TileSet =
        customTiles(AREA) { npc, target, _ ->
            val centre = npc.coords.translate(Moon.BOSS_SIZE / 2, Moon.BOSS_SIZE / 2)
            val reach =
                maxOf(abs(target.coords.x - centre.x), abs(target.coords.z - centre.z))
                    .coerceAtLeast(Moon.BOSS_SIZE / 2 + 1)
            perimeter(-reach, reach).map { (dx, dz) -> centre.translate(dx, dz) }
        }

    fun pool(tile: TargetExpr.Single, maxHit: Int, despawnAnim: String, despawnAfter: Int): Effect =
        parallel(
            spawnLoc(POOL_LOC, tile),
            locAnim(tile, POOL_IDLE),
            after(despawnAfter, locAnim(tile, despawnAnim)),
            after(despawnAfter, onEach(playersOn(tile), external(MoonsExternals.POOL_HIT, params = maxHit))),
            after(POOL_LIFETIME, removeLoc(tile)),
        )
}

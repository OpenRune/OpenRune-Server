package org.rsmod.content.bosses.moons.blood

import org.rsmod.api.bosses.dsl.CurrentTile
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.area
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.invulnerable
import org.rsmod.api.bosses.dsl.locAnim
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.onTiles
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.randomFreeTiles
import org.rsmod.api.bosses.dsl.removeLoc
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.setPlayerVar
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.spawnLoc
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.vulnerable
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals

internal object BloodRain {
    private const val THROW_TICK = 3
    private const val FIRE_TICK = 4
    private const val DISAPPEAR_TICK = 8
    private const val HIDE_TICK = 10
    private const val RETURN_TICK = 26
    private const val FIRE_REMOVE_TICK = 30
    private const val POOL_FIRST_TICK = 7
    private const val POOL_WAVES = 19
    private const val POOL_MAX_HIT = 9
    private const val POOL_DESPAWN_AFTER = 4
    private const val FIRE_LOC = "loc.pmoon_boss_blood_fire"
    private const val FIRE_EDGE_TILES = 15
    private const val FIRE_FIRST_EDGE_MIN = -6
    private const val FIRE_FIRST_EDGE_MAX = 10
    private const val POOL_DESPAWN_SEQ = "seq.vfx_djinn_blood_pool_despawn_01"
    private val POOLS_PER_WAVE = 18..25
    private val FIRE_SEQS =
        listOf(
            "seq.fx_fire02_spawn_idle",
            "seq.fx_fire02_spawn_idle_delay_1",
            "seq.fx_fire02_spawn_idle_delay_2",
            "seq.fx_fire02_spawn_idle_delay_3",
            "seq.fx_fire02_spawn_idle_delay_4",
        )

    private val RAIN_AREA =
        area(
            spawnTile(FIRE_FIRST_EDGE_MIN + 1, FIRE_FIRST_EDGE_MIN + 1),
            spawnTile(FIRE_FIRST_EDGE_MAX - 1, FIRE_FIRST_EDGE_MAX - 1),
        )

    private val FIRE_TILES = BloodArena.perimeter(FIRE_FIRST_EDGE_MIN, FIRE_FIRST_EDGE_MAX)

    private fun fire(dx: Int, dz: Int): Effect {
        val tile = spawnTile(dx, dz)
        val onRow = (dz == FIRE_FIRST_EDGE_MIN || dz == FIRE_FIRST_EDGE_MAX) && dx != FIRE_FIRST_EDGE_MIN && dx != FIRE_FIRST_EDGE_MAX
        val along = if (onRow) dx else dz
        val step = (along - FIRE_FIRST_EDGE_MIN - 1).coerceIn(0, FIRE_EDGE_TILES - 1)
        val distance = minOf(step, FIRE_EDGE_TILES - 1 - step).coerceAtMost(FIRE_SEQS.lastIndex)
        return parallel(
            spawnLoc(FIRE_LOC, tile, angle = if (onRow) 1 else 0),
            locAnim(tile, FIRE_SEQS[distance]),
        )
    }

    private fun lightFire(): Effect = parallel(*FIRE_TILES.map { (dx, dz) -> fire(dx, dz) }.toTypedArray())

    private fun putOutFire(): Effect =
        parallel(*FIRE_TILES.map { (dx, dz) -> removeLoc(spawnTile(dx, dz)) }.toTypedArray())

    private fun poolWave(): Effect =
        onTiles(
            randomFreeTiles(RAIN_AREA, POOLS_PER_WAVE),
            BloodArena.pool(CurrentTile, POOL_MAX_HIT, POOL_DESPAWN_SEQ, POOL_DESPAWN_AFTER),
        )

    private fun hideMoon(hidden: Int): Effect =
        onEach(playersIn(BloodArena.AREA), setPlayerVar(BloodArena.HIDE_NPC, hidden))

    val timeline: Effect =
        parallel(
            sequence(
                invulnerable(),
                external(MoonsExternals.FOCUS, params = Moon.FULL_MOON),
                sound(BloodArena.HEAL_SYNTH),
                wait(1),
                sound(BloodArena.HEAL_SYNTH),
                wait(THROW_TICK - 1),
                anim("seq.npc_djinn_01_magic"),
                external(MoonsExternals.RAIN_THROW),
                wait(FIRE_TICK - THROW_TICK),
                lightFire(),
                wait(DISAPPEAR_TICK - FIRE_TICK),
                anim("seq.npc_djinn_01_teleport_idle_disappear"),
                wait(HIDE_TICK - DISAPPEAR_TICK),
                hideMoon(1),
                wait(RETURN_TICK - HIDE_TICK),
                anim("seq.npc_djinn_01_teleport_idle_appear"),
                hideMoon(0),
                vulnerable(),
                wait(FIRE_REMOVE_TICK - RETURN_TICK),
                putOutFire(),
            ),
            sequence(
                wait(POOL_FIRST_TICK),
                repeat(POOL_WAVES, gap = 1, effect = poolWave()),
            ),
        )
}

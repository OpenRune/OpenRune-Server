package org.rsmod.content.bosses.moons.blue

import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.invulnerable
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.setPlayerVar
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.soundTo
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals
import org.rsmod.content.bosses.moons.arena

internal object BlueStorm {
    private const val FIRE_TICK = 1
    private const val SPAWN_FIRST = 2
    private const val SPAWN_LAST = 42
    private const val STEP_LAST = 49
    private const val CLEAR_TICK = 50
    private const val CAST_TICK = 3
    private const val BRAZIER_TICK = 7
    private const val HEAL_GAP = 10
    private const val HEALS = 5
    private const val FIRE_OFF_TICK = 65
    private const val START_SYNTH = "synth.varl_pm_boss_blue_spike_wall_spawn_01"
    private const val SHROUD_SYNTH = "synth.varl_pm_boss_blue_magic_extinguish_01"
    private val LANDING = Moon.BLUE_STORM_LANDING

    private fun build(): Effect {
        val fighters = playersIn(Moon.Blue.arena())
        return parallel(
            invulnerable(),
            external(MoonsExternals.FOCUS, params = Moon.NEW_MOON),
            sound(START_SYNTH, at = spawnTile(2, 2)),
            after(FIRE_TICK, external(MoonsExternals.FIRE_ON)),
            *(SPAWN_FIRST..SPAWN_LAST)
                .map { after(it, external(MoonsExternals.STORM_SPAWN)) }
                .toTypedArray(),
            *(SPAWN_FIRST..STEP_LAST)
                .map { after(it, external(MoonsExternals.STORM_STEP, params = false)) }
                .toTypedArray(),
            after(CLEAR_TICK, external(MoonsExternals.STORM_STEP, params = true)),
            after(
                CAST_TICK,
                parallel(
                    external(MoonsExternals.THROW_TO, params = LANDING),
                    external(MoonsExternals.STORM_CAST),
                ),
            ),
            after(
                BRAZIER_TICK,
                onEach(
                    fighters,
                    parallel(
                        setPlayerVar(BlueFights.BRAZIER_LEFT, 1),
                        setPlayerVar(BlueFights.BRAZIER_RIGHT, 1),
                        soundTo(SHROUD_SYNTH),
                    ),
                ),
            ),
            *(1..HEALS)
                .map { after(HEAL_GAP * it, onEach(fighters, external(MoonsExternals.STORM_HEAL))) }
                .toTypedArray(),
            after(FIRE_OFF_TICK, external(MoonsExternals.FIRE_OFF)),
        )
    }

    val timeline: Effect = build()
}

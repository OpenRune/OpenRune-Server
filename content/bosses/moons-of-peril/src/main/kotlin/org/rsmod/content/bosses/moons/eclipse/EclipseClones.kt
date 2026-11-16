package org.rsmod.content.bosses.moons.eclipse

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.invulnerable
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.removeLoc
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.spawnLoc
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.dsl.vulnerable
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals

internal object EclipseClones {
    private const val BIND_TICK = 2
    private const val DISAPPEAR_TICK = 4
    private const val HIDE_TICK = 6
    private const val FIRST_WAVE_TICK = 10
    private const val WAVES = 5
    private const val WAVE_PERIOD = 9
    private const val CLONE_GAP = 2
    private const val CLONE_TICKS = 7
    private const val STRIKE_AFTER = 3
    private const val LEAVE_AFTER = 5
    private const val RELEASE_TICK = 54
    private const val RETURN_TICK = 55
    private const val CLONE_NPC = "npc.pmoon_boss_eclipse_clone"
    private const val CIRCLE_LOC = "loc.pmoon_boss_small_threat_circle"
    private val CIRCLE_TILE = spawnTile(Moon.BOSS_SIZE / 2, Moon.BOSS_SIZE / 2)

    private fun clone(side: Side): Effect =
        parallel(
            summon(
                CLONE_NPC,
                radius = 0,
                centeredOn = spawnTile(side.dx, side.dz),
                mode = NpcMode.None,
                duration = CLONE_TICKS,
                onSummon = MoonsExternals.CLONE_SPAWN,
                onSummonParams = side,
            ),
            after(STRIKE_AFTER, external(MoonsExternals.CLONE_STRIKE, params = side)),
            after(LEAVE_AFTER, external(MoonsExternals.CLONE_LEAVE, params = side)),
        )

    private fun wave(a: Side, b: Side, c: Side): Effect =
        parallel(clone(a), after(CLONE_GAP, clone(b)), after(CLONE_GAP * 2, clone(c)))

    private val WAVES_OF_THREE: List<Effect> =
        Side.entries.flatMap { a ->
            Side.entries.filter { it != a }.flatMap { b ->
                Side.entries.filter { it != a && it != b }.map { c -> wave(a, b, c) }
            }
        }

    val timeline: Effect =
        parallel(
            sequence(
                invulnerable(),
                external(MoonsExternals.OPS_HIDE),
                external(MoonsExternals.FOCUS, params = Moon.FULL_MOON),
                anim("seq.npc_djinn_02_magic"),
                spawnLoc(CIRCLE_LOC, CIRCLE_TILE),
                wait(BIND_TICK),
                external(MoonsExternals.BIND),
                wait(DISAPPEAR_TICK - BIND_TICK),
                anim("seq.npc_djinn_02_teleport_idle_disappear"),
                wait(HIDE_TICK - DISAPPEAR_TICK),
                external(MoonsExternals.MOON_HIDE),
                wait(RELEASE_TICK - HIDE_TICK),
                removeLoc(CIRCLE_TILE),
                external(MoonsExternals.UNBIND),
                wait(RETURN_TICK - RELEASE_TICK),
                external(MoonsExternals.MOON_SHOW),
                anim("seq.npc_djinn_02_teleport_idle_appear"),
                external(MoonsExternals.OPS_SHOW),
                vulnerable(),
            ),
            sequence(
                wait(FIRST_WAVE_TICK),
                repeat(WAVES, gap = WAVE_PERIOD, effect = oneOf(WAVES_OF_THREE)),
            ),
        )
}

package org.rsmod.content.bosses.moons.eclipse

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.invulnerable
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.removeLoc
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.spawnLoc
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.dsl.vulnerable
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals
import org.rsmod.content.bosses.moons.arena

internal object EclipseRays {
    private const val THROW_TICK = 2
    private const val WALK_START = 10
    private const val WALK_STEPS = 37
    private const val RAY_COUNT = 13
    private const val RAY_PERIOD = 3
    private const val UNFACE_TICK = 43
    private const val VULNERABLE_TICK = 49
    private const val SINK_TICK = 50
    private const val BLOCKER_REMOVE_TICK = 52
    private const val SHIELD_TICKS = 52
    private const val CIRCLE_TICKS = 6 + 12 + 1
    private const val SHIELD_NPC = "npc.pmoon_boss_eclipse_moon_shield"
    private const val BLOCKER_LOC = "loc.pmoon_boss_eclipse_pathing_blocker"
    private const val CAST_SEQ = "seq.npc_djinn_02_magic"
    private const val RAY_SEQ = "seq.npc_djinn_02_magic_02"
    private const val CHARGE_SPOTANIM = "spotanim.vfx_djinn_eclipse_moon_spotanim_magic"
    private const val CHARGE_DELAY = 5
    private const val SHIELD_RISE_SYNTH = "synth.varl_pm_eclipse_sphere_spawn_07"
    private const val SOUND_RADIUS = 15
    private val SHIELD_START = spawnTile(5, -2)
    private val BLOCKER_TILE = spawnTile(-2, -2)
    private val THROW_DEST = Moon.ECLIPSE_RAYS_LANDING

    private fun ray(): Effect =
        sequence(
            anim(RAY_SEQ),
            spotanim(CHARGE_SPOTANIM, delay = CHARGE_DELAY),
            after(1, onEach(playersIn(Moon.Eclipse.arena()), external(MoonsExternals.RAY_FIRE))),
        )

    val timeline: Effect =
        parallel(
            sequence(
                invulnerable(),
                external(MoonsExternals.OPS_HIDE),
                external(MoonsExternals.FOCUS, params = Moon.NEW_MOON),
                anim(CAST_SEQ),
                summon(
                    SHIELD_NPC,
                    radius = 0,
                    centeredOn = SHIELD_START,
                    mode = NpcMode.None,
                    duration = SHIELD_TICKS,
                    onSummon = MoonsExternals.SHIELD_SPAWN,
                ),
                sound(SHIELD_RISE_SYNTH, radius = SOUND_RADIUS, at = SHIELD_START),
                wait(THROW_TICK),
                external(MoonsExternals.THROW_TO, params = THROW_DEST),
                spawnLoc(BLOCKER_LOC, BLOCKER_TILE),
                wait(UNFACE_TICK - THROW_TICK),
                external(MoonsExternals.SHIELD_UNFACE),
                wait(VULNERABLE_TICK - UNFACE_TICK),
                external(MoonsExternals.OPS_SHOW),
                vulnerable(),
                summon(
                    "npc.pmoon_boss_threat_circle",
                    radius = 0,
                    centeredOn = spawnTile(Moon.GLYPH_OFFSETS[1].first, Moon.GLYPH_OFFSETS[1].second),
                    mode = NpcMode.None,
                    duration = CIRCLE_TICKS,
                    onSummon = MoonsExternals.CIRCLE_SPAWN,
                ),
                wait(SINK_TICK - VULNERABLE_TICK),
                external(MoonsExternals.SHIELD_SINK),
                wait(BLOCKER_REMOVE_TICK - SINK_TICK),
                removeLoc(BLOCKER_TILE),
            ),
            sequence(
                wait(WALK_START),
                repeat(WALK_STEPS, gap = 1, effect = external(MoonsExternals.SHIELD_STEP)),
            ),
            sequence(wait(WALK_START), repeat(RAY_COUNT, gap = RAY_PERIOD, effect = ray())),
        )
}

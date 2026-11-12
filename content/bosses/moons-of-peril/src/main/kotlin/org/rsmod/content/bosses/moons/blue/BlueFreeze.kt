package org.rsmod.content.bosses.moons.blue

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.invulnerable
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.removeLoc
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.setPlayerVar
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.spawnLoc
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.dsl.vulnerable
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.content.bosses.moons.arena

internal object BlueFreeze {
    private const val STAND_IN_TICK = 2
    private const val DISARM_TICK = 4
    private const val FADE_TICK = 5
    private const val DISAPPEAR_TICK = 6
    private const val HIDE_TICK = 8
    private const val WAVE_GAP = 8
    private const val WAVES = 6
    private const val SELF_DAMAGE_FIRST = 11
    private const val EXPLODE_TICK = 51
    private const val CIRCLE_TICK = 52
    private const val CIRCLE_TICKS = 19
    private const val RETURN_TICK = 55
    private const val FOCUS_TICK = 56
    private const val END_TICK = 57
    private const val ICICLE_TICKS = 50
    private const val SOUND_RADIUS = 10
    private const val DISAPPEAR_SEQ = "seq.npc_djinn_03_teleport_idle_disappear"
    private const val APPEAR_SEQ = "seq.npc_djinn_03_teleport_idle_appear"
    private const val RISE_SYNTH = "synth.varl_pm_boss_blue_weapon_ice_block"
    private const val EXPLODE_SYNTH = "synth.varl_pm_boss_blue_weapon_ice_block_despawn"
    private const val HIDE_NPC = "varbit.pmoon_boss_hide_npc"
    private val OVERLAYS =
        listOf(
            "loc.pmoon_boss_overlay_new_blocking",
            "loc.pmoon_boss_overlay_quarter_waxing_blocking",
            "loc.pmoon_boss_overlay_half_waxing_blocking",
        )

    private fun tile(index: Int) =
        BlueFights.BLOCK_OFFSETS[index].let { (dx, dz) -> spawnTile(dx, dz) }

    private fun build(): Effect {
        val fighters = playersIn(Moon.Blue.arena())
        val blocks = BlueFights.BLOCK_OFFSETS.indices
        return parallel(
            sequence(
                invulnerable(),
                external(MoonsExternals.FREEZE_START),
                external(MoonsExternals.OPS_HIDE),
                external(MoonsExternals.FOCUS, params = 4),
                onEach(fighters, external(MoonsExternals.FREEZE_ARM)),
            ),
            after(
                STAND_IN_TICK,
                sequence(
                    onEach(
                        fighters,
                        parallel(setPlayerVar(HIDE_NPC, 1), external(MoonsExternals.STAND_IN)),
                    ),
                    *blocks
                        .map {
                            summon(
                                MoonsFights.ICICLE_NPCS[it],
                                radius = 0,
                                centeredOn = tile(it),
                                mode = NpcMode.None,
                                duration = ICICLE_TICKS,
                                onSummon = MoonsExternals.ICICLE_SPAWN,
                                onSummonParams = it,
                            )
                        }
                        .toTypedArray(),
                    *blocks.map { spawnLoc(OVERLAYS[it], tile(it)) }.toTypedArray(),
                    *blocks.map { sound(RISE_SYNTH, radius = SOUND_RADIUS, at = tile(it)) }.toTypedArray(),
                ),
            ),
            after(DISARM_TICK, onEach(fighters, external(MoonsExternals.DISARM))),
            after(FADE_TICK, onEach(fighters, external(MoonsExternals.STAND_IN_FADE))),
            after(DISAPPEAR_TICK, anim(DISAPPEAR_SEQ)),
            after(
                HIDE_TICK,
                sequence(
                    onEach(
                        fighters,
                        parallel(setPlayerVar(HIDE_NPC, 0), external(MoonsExternals.STAND_IN_REMOVE)),
                    ),
                    external(MoonsExternals.MOON_HIDE_BLUE),
                ),
            ),
            *(0 until WAVES)
                .map { after(HIDE_TICK + WAVE_GAP * it, external(MoonsExternals.ICE_FLOOR, params = it)) }
                .toTypedArray(),
            *(0 until WAVES)
                .map {
                    after(
                        SELF_DAMAGE_FIRST + WAVE_GAP * it,
                        onEach(fighters, external(MoonsExternals.FREEZE_SELF_DAMAGE)),
                    )
                }
                .toTypedArray(),
            after(
                EXPLODE_TICK,
                sequence(
                    external(MoonsExternals.ICICLES_EXPLODE),
                    sound(EXPLODE_SYNTH, radius = SOUND_RADIUS),
                    onEach(fighters, external(MoonsExternals.FREEZE_EXPLODE)),
                ),
            ),
            after(
                CIRCLE_TICK,
                sequence(
                    *blocks.map { removeLoc(tile(it)) }.toTypedArray(),
                    summon(
                        "npc.pmoon_boss_threat_circle",
                        radius = 0,
                        centeredOn = Moon.GLYPH_OFFSETS[5].let { (dx, dz) -> spawnTile(dx, dz) },
                        mode = NpcMode.None,
                        duration = CIRCLE_TICKS,
                        onSummon = MoonsExternals.CIRCLE_SPAWN,
                    ),
                ),
            ),
            after(
                RETURN_TICK,
                sequence(
                    external(MoonsExternals.MOON_SHOW),
                    anim(APPEAR_SEQ),
                    external(MoonsExternals.OPS_SHOW),
                    vulnerable(),
                ),
            ),
            after(FOCUS_TICK, onEach(fighters, external(MoonsExternals.PLAYER_FOCUS, params = 4))),
            after(END_TICK, onEach(fighters, external(MoonsExternals.FREEZE_END))),
        )
    }

    val timeline: Effect = build()
}

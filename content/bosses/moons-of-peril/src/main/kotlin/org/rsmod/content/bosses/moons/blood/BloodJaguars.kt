package org.rsmod.content.bosses.moons.blood

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.dsl.Centre
import org.rsmod.api.bosses.dsl.CurrentTile
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.invulnerable
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.onTiles
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.setPlayerVar
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.dsl.vulnerable
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals
import org.rsmod.content.bosses.moons.MoonsFights

internal object BloodJaguars {
    private const val AD_HP_START = 25
    private const val JAGUAR_NPC = "npc.pmoon_boss_jaguar"
    private const val JAGUAR_TICKS = 45
    private const val OVERLAY_TICKS = 41
    private const val SPAWN_TICK = 2
    private const val VULNERABLE_TICK = 47
    private const val POOL_FIRST_TICK = 7
    private const val POOL_WAVES = 7
    private const val POOL_PERIOD = 6
    private const val POOL_MAX_HIT = 6
    private const val POOL_DESPAWN_AFTER = 3
    private const val ATTACK_FIRST_TICK = 10
    private const val ATTACK_COUNT = 6
    private const val ATTACK_PERIOD = 6
    private const val RAISE_SPOTANIM = "spotanim.fx_wardens_charging01"
    private const val RAISE_SYNTH = "synth.varl_pm_blood_jaguar_spawn_04"
    private const val POOL_DESPAWN_SEQ = "seq.vfx_djinn_blood_pool_despawn_02"
    private const val DRAINED_MESSAGE =
        "<col=ff3045>You suddenly feel drained. A blood jaguar is shaping from your blood!</col>"
    private val RAISE = ProjectileConfig.fixed(350, 0, 0, 50, 32)

    private val OVERLAY_NPCS =
        listOf(
            "npc.pmoon_boss_overlay_new",
            "npc.pmoon_boss_overlay_quarter_waxing",
            "npc.pmoon_boss_overlay_half_waxing",
            "npc.pmoon_boss_overlay_threequarters_waxing",
            "npc.pmoon_boss_overlay_full",
            "npc.pmoon_boss_overlay_threequarter_waning",
            "npc.pmoon_boss_overlay_half_waning",
            "npc.pmoon_boss_overlay_quarter_waning",
        )

    private val RING = Moon.JAGUAR_OFFSETS.map { (dx, dz) -> spawnTile(dx, dz) }

    private fun safeGlyph(glyph: Int): Effect {
        val (dx, dz) = Moon.GLYPH_OFFSETS[glyph]
        return sequence(
            onEach(
                playersIn(BloodArena.AREA),
                parallel(
                    setPlayerVar(MoonsFights.CURRENT_FOCUS, glyph),
                    setPlayerVar(BloodArena.AD_HP, AD_HP_START),
                ),
            ),
            after(
                SPAWN_TICK,
                summon(
                    OVERLAY_NPCS[glyph],
                    radius = 0,
                    centeredOn = spawnTile(dx, dz),
                    mode = NpcMode.None,
                    duration = OVERLAY_TICKS,
                ),
            ),
        )
    }

    private fun raise(): Effect =
        parallel(
            *RING.map { tile ->
                    projectile(RAISE_SPOTANIM, config = RAISE, from = Centre, target = tile)
                }
                .toTypedArray()
        )

    private fun spawnJaguars(): Effect =
        parallel(
            *RING.map { tile ->
                    summon(
                        JAGUAR_NPC,
                        radius = 0,
                        centeredOn = tile,
                        mode = NpcMode.None,
                        duration = JAGUAR_TICKS,
                        onSummon = MoonsExternals.JAGUAR_SPAWN,
                        onSummonParams = Moon.Blood,
                    )
                }
                .toTypedArray()
        )

    private fun poolWave(): Effect =
        onEach(
            playersIn(BloodArena.AREA),
            onTiles(
                BloodArena.ringAroundPlayer(),
                BloodArena.pool(CurrentTile, POOL_MAX_HIT, POOL_DESPAWN_SEQ, POOL_DESPAWN_AFTER),
            ),
        )

    val timeline: Effect =
        parallel(
            sequence(
                invulnerable(),
                external(MoonsExternals.FOCUS, params = Moon.NEW_MOON),
                oneOf(Moon.SAFE_GLYPHS.map(::safeGlyph)),
                raise(),
                sound(BloodArena.HEAL_SYNTH),
                wait(1),
                sound(RAISE_SYNTH),
                wait(SPAWN_TICK - 1),
                spawnJaguars(),
                external(MoonsExternals.OVERLAYS),
                wait(1),
                onEach(playersIn(BloodArena.AREA), message(DRAINED_MESSAGE)),
                anim("seq.npc_djinn_01_magic"),
                external(MoonsExternals.SAP),
                wait(VULNERABLE_TICK - SPAWN_TICK - 1),
                vulnerable(),
            ),
            sequence(
                wait(POOL_FIRST_TICK),
                repeat(POOL_WAVES, gap = POOL_PERIOD, effect = poolWave()),
            ),
            sequence(
                wait(ATTACK_FIRST_TICK),
                repeat(ATTACK_COUNT, gap = ATTACK_PERIOD, effect = external(MoonsExternals.JAGUAR_ATTACK)),
            ),
        )
}

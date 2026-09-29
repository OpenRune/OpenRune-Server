package org.rsmod.content.bosses.leviathan

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object LeviathanSpecials {
    private val arenaPlayers = playersIn(LeviathanRockfall.ARENA)

    private const val RECOVERY = 2

    private const val LIGHTNING_MESSAGE = "<col=a53fff>The Leviathan charges up a lightning attack..."
    private const val LIGHTNING_CHARGE_SYNTH = "synth.leviathan_lightning_charge"
    private const val BEAM_START_SEQ = "seq.npc_leviathan_01_360_start"
    private const val BEAM_LOOP_SEQ = "seq.npc_leviathan_01_360_loop"
    private const val BEAM_END_SEQ = "seq.npc_leviathan_01_360_end"
    private const val BEAM_SPOTANIM = "spotanim.beam_360_attack_vfx_01"
    private const val BEAM_START = 3
    private const val LIGHTNING_END = BEAM_START + 35
    private const val LIGHTNING_ORB_INTERVAL = 4
    private val LIGHTNING_ORB_COUNT = 1..3
    private const val LIGHTNING_PLAYER_CHANCE = 3
    private val LIGHTNING_DAMAGE = 20..30
    private const val LIGHTNING_ORB_SPOTANIM = "spotanim.dt2_leviathan_bomb01"
    private const val LIGHTNING_STRIKE_SPOTANIM = "spotanim.vfx_leviathan_360_lightning"
    private const val LIGHTNING_STRIKE_SYNTH = "synth.leviathan_orb_magic"
    private const val LIGHTNING_ORB_CYCLES = 40 + 110
    private val LIGHTNING_ORB_CONFIG =
        ProjectileConfig.fixed(startHeight = 800, endHeight = 0, delay = 40, travel = 110, angle = 3)
    private const val SHADOW_SPOTANIM = "spotanim.gargboss_debris_shadow_90"
    private const val SHADOW_LEAD = 60

    private const val SMOKE_MESSAGE = "<col=a53fff>The Leviathan begins to spit out debris..."
    private const val SPIT_START_SEQ = "seq.npc_leviathan_01_spit_start"
    private const val SPIT_LOOP_SEQ = "seq.npc_leviathan_01_spit_loop"
    private const val EXPLOSION_SEQ = "seq.npc_leviathan_01_explosion_start"
    private const val EXPLOSION_SPOTANIM = "spotanim.spotanim_leviathan_explode_02"
    private const val SPIT_START_SYNTH = "synth.leviathan_spit_start"
    private const val SPIT_LAUNCH_SYNTH = "synth.leviathan_spit_launch"
    private const val SMOKE_SPIT_START = 4
    private const val SMOKE_SPITS = 10
    private const val SMOKE_BLAST_DELAY = 2
    private const val SMOKE_END_DELAY = 3
    private const val DEBRIS_PROJECTILE = "spotanim.projanim_brain_01"
    private const val DEBRIS_IMPACT_SPOTANIM = "spotanim.projanim_brain_01_impact_01"
    private val DEBRIS_CONFIG =
        ProjectileConfig.fixed(startHeight = 600, endHeight = 0, delay = 0, travel = 60, angle = 40)
    private const val DEBRIS_LAND_TICKS = 2

    private val STRIPS =
        LeviathanArena.LIGHTNING_STRIPS.map {
            area(LeviathanArena.relative(it.minX, it.minZ), LeviathanArena.relative(it.maxX, it.maxZ))
        }

    fun start(lightning: AbilityRef, smoke: AbilityRef): Effect =
        sequence(
            whenever(
                varnIs(LeviathanVarns.NEXT_SPECIAL, Special.NONE),
                oneOf(Special.entries.map { setVarn(LeviathanVarns.NEXT_SPECIAL, it.varnValue) }),
            ),
            switch(
                LeviathanVarns.NEXT_SPECIAL,
                Special.Lightning.varnValue to
                    sequence(setVarn(LeviathanVarns.NEXT_SPECIAL, Special.Smoke.varnValue), run(lightning)),
                Special.Smoke.varnValue to
                    sequence(setVarn(LeviathanVarns.NEXT_SPECIAL, Special.Lightning.varnValue), run(smoke)),
            ),
        )

    fun lightning(volley: AbilityRef): Effect {
        val windup = external(LeviathanFights.BEAM_WINDUP_EXT)
        val sweep =
            sequence(anim(BEAM_LOOP_SEQ), spotanim(BEAM_SPOTANIM), external(LeviathanFights.BEAM_FIRE_EXT))
        val ticks =
            (0 until LIGHTNING_END).flatMap { tick ->
                listOfNotNull(
                    if (tick % LIGHTNING_ORB_INTERVAL == 0) lightningOrbs() else null,
                    if (tick < BEAM_START) windup else sweep,
                    wait(1),
                )
            }
        return Effect.Sequence(
            listOf(
                external(LeviathanFights.BEAM_AIM_EXT),
                message(LIGHTNING_MESSAGE, arenaPlayers),
                soundTo(LIGHTNING_CHARGE_SYNTH, arenaPlayers),
                anim(BEAM_START_SEQ),
                spotanim(BEAM_SPOTANIM),
            ) +
                ticks +
                listOf(anim(BEAM_END_SEQ), wait(1), finish(volley, RECOVERY + 1))
        )
    }

    fun smoke(volley: AbilityRef): Effect =
        sequence(
            faceTarget(),
            anim(LeviathanRockfall.SEQ),
            message(SMOKE_MESSAGE, arenaPlayers),
            soundTo(LeviathanRockfall.SYNTH, arenaPlayers, delay = 5),
            wait(SMOKE_SPIT_START),
            anim(SPIT_START_SEQ),
            soundTo(SPIT_START_SYNTH, arenaPlayers),
            wait(1),
            repeat(SMOKE_SPITS, effect = sequence(spit(), wait(1))),
            wait(SMOKE_BLAST_DELAY),
            anim(EXPLOSION_SEQ),
            spotanim(EXPLOSION_SPOTANIM),
            external(LeviathanFights.SMOKE_WAVE_EXT),
            wait(SMOKE_END_DELAY),
            finish(volley, RECOVERY + 2),
        )

    private fun finish(volley: AbilityRef, recovery: Int): Effect =
        sequence(setVarn(LeviathanVarns.IN_SPECIAL, 0), faceTarget(), forceNext(volley), wait(recovery))

    private fun lightningOrbs(): Effect =
        Effect.Sequence(
            STRIPS.map { onTiles(randomFreeTiles(it, LIGHTNING_ORB_COUNT), lightningOrb) } +
                chance(
                    LIGHTNING_PLAYER_CHANCE,
                    Effect.Sequence(STRIPS.map { onTiles(tilesUnderPlayers(it), lightningOrb) }),
                )
        )

    private val lightningOrb: Effect =
        sequence(
            projectile(LIGHTNING_ORB_SPOTANIM, target = EachTile, from = Centre, config = LIGHTNING_ORB_CONFIG),
            mapSpotanim(SHADOW_SPOTANIM, EachTile, delay = LIGHTNING_ORB_CYCLES - SHADOW_LEAD),
            mapSpotanim(LIGHTNING_STRIKE_SPOTANIM, EachTile, delay = LIGHTNING_ORB_CYCLES),
            sound(LIGHTNING_STRIKE_SYNTH, radius = 3, at = EachTile, delay = LIGHTNING_ORB_CYCLES),
            after(
                LIGHTNING_ORB_CYCLES / 30,
                hit {
                    target = playersOn(EachTile)
                    delay = 1
                    damage(LIGHTNING_DAMAGE).roll()
                    type(Typeless)
                    hazard()
                },
            ),
        )

    private fun spit(): Effect =
        sequence(
            anim(SPIT_LOOP_SEQ),
            onTiles(
                nearestFreeTiles(listOf(CurrentTargetTile), LeviathanRockfall.ARENA, searchRadius = 0),
                sequence(
                    projectile(
                        DEBRIS_PROJECTILE,
                        target = EachTile,
                        from = toward(Centre, EachTile, LeviathanOrbs.SOURCE_OFFSET),
                        config = DEBRIS_CONFIG,
                    ),
                    mapSpotanim(SHADOW_SPOTANIM, EachTile),
                    soundTo(SPIT_LAUNCH_SYNTH, arenaPlayers),
                    after(
                        DEBRIS_LAND_TICKS,
                        onTiles(
                            nearestFreeTiles(listOf(EachTile), LeviathanRockfall.ARENA, searchRadius = 0),
                            sequence(mapSpotanim(DEBRIS_IMPACT_SPOTANIM, EachTile), LeviathanRockfall.rubbleLanding),
                        ),
                    ),
                ),
            ),
        )
}

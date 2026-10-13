package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.dsl.CurrentTargetTile
import org.rsmod.api.bosses.dsl.CurrentTile
import org.rsmod.api.bosses.dsl.ImpactTile
import org.rsmod.api.bosses.dsl.InPhase
import org.rsmod.api.bosses.dsl.Now
import org.rsmod.api.bosses.dsl.Typeless
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.bound
import org.rsmod.api.bosses.dsl.choose
import org.rsmod.api.bosses.dsl.customTile
import org.rsmod.api.bosses.dsl.customTiles
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.faceTile
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.lockMovement
import org.rsmod.api.bosses.dsl.mapSpotanim
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.not
import org.rsmod.api.bosses.dsl.offset
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.onTiles
import org.rsmod.api.bosses.dsl.playersOn
import org.rsmod.api.bosses.dsl.plus
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.setVarn
import org.rsmod.api.bosses.dsl.tile
import org.rsmod.api.bosses.dsl.unlockMovement
import org.rsmod.api.bosses.dsl.varnAtLeast
import org.rsmod.api.bosses.dsl.varnIs
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.dsl.weightedRandom
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.dsl.withTile
import org.rsmod.api.bosses.dsl.withTiles
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.challengeArea
import org.rsmod.content.raids.toa.raid.encounter.eachTarget
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.content.raids.toa.raid.encounter.roomDamage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal object BabaSpecials {
    const val SPECIAL_ROCKFALL = 1
    const val SPECIAL_SHOCKWAVE = 2
    const val SPECIAL_DISCHARGE = 3

    private const val MARK = "mark"
    private const val NORTH = "north"
    private const val SOUTH = "south"
    private const val CENTRE = "centre"
    private const val RING = "ring"
    private const val FALL_BASE = BabaRocks.BASE_FALL_TICKS
    private const val FALL_STEP = BabaRocks.TICKS_PER_TIER

    private val rolling = InPhase(BabaPhases.ROLLING)
    private val deeperPath = roomCondition<BabaEncounter> { room, _ -> room.pathTier >= 2 }
    private val pathStarted = roomCondition<BabaEncounter> { room, _ -> room.pathTier >= 1 }
    private val shaking =
        roomCondition<BabaEncounter> { room, _ -> room.raid.isActive(ToaInvocationKey.ShakingThingsUp) }
    private val rockfallReady = roomCondition<BabaEncounter> { room, npc -> room.rockfallReady(npc) }
    private val noBaboons = roomCondition<BabaEncounter> { room, _ -> room.baboons.alive == 0 }

    private val ROCK_CONFIG = ProjectileConfig.fixed(100, 31, 90, 120, 40, progress = 64)

    private val targetCentre: (Npc, Player) -> CoordGrid = { _, target -> target.coords }
    private val bossCentre: (Npc, Player) -> CoordGrid = { npc, _ ->
        npc.coords.translate(BabaShock.BOSS_CENTRE, BabaShock.BOSS_CENTRE)
    }

    fun special(): Effect =
        whenever(
            rolling,
            Effect.NoOp,
            whenever(
                varnIs(BabaVarns.AUTOS, 1),
                run(BabaAbilities.ROCKFALL),
                choose(
                    weightedRandom {
                        +random(
                            BabaAbilities.ROCKFALL,
                            1,
                            requires = rockfallReady and !varnIs(BabaVarns.LAST_SPECIAL, SPECIAL_ROCKFALL),
                        )
                        +random(
                            BabaAbilities.SHOCKWAVE,
                            1,
                            requires = !varnIs(BabaVarns.LAST_SPECIAL, SPECIAL_SHOCKWAVE),
                        )
                        +random(
                            BabaAbilities.DISCHARGE,
                            1,
                            requires = noBaboons and !varnIs(BabaVarns.LAST_SPECIAL, SPECIAL_DISCHARGE),
                        )
                    },
                    mapOf(
                        BabaAbilities.ROCKFALL to run(BabaAbilities.ROCKFALL),
                        BabaAbilities.SHOCKWAVE to run(BabaAbilities.SHOCKWAVE),
                        BabaAbilities.DISCHARGE to run(BabaAbilities.DISCHARGE),
                    ),
                ),
            ),
        )

    fun specialSlot(): Effect =
        oneOf((BabaRocks.CAST_MIN..BabaRocks.CAST_MAX).map { after(it, run(BabaAbilities.SPECIAL)) })

    fun rockfall(): Effect =
        sequence(
            setVarn(BabaVarns.LAST_SPECIAL, SPECIAL_ROCKFALL),
            setVarn(BabaVarns.ROCKFALL_READY, Now + BabaRocks.COOLDOWN),
            setVarn(BabaVarns.THROW_AT, Now + BabaRocks.THROW_DELAY),
            anim(BabaSeqs.MELEE),
            eachTarget(withTile(MARK, CurrentTargetTile, marker(tile(MARK), rubble = false))),
            withTile(NORTH, edgeTile(BabaRocks.NORTH_Z), marker(tile(NORTH), rubble = true)),
            withTile(SOUTH, edgeTile(BabaRocks.SOUTH_Z), marker(tile(SOUTH), rubble = true)),
        )

    fun rockThrow(): Effect =
        sequence(
            setVarn(BabaVarns.THROW_AT, 0),
            anim(BabaSeqs.THROW),
            eachTarget(
                sequence(
                    message(BabaRocks.THROW_TEXT),
                    projectile(spotanim = BabaSpots.ROCK_TRAVEL, config = ROCK_CONFIG),
                    after(
                        BabaRocks.THROW_RESOLVE_DELAY,
                        whenever(rolling, Effect.NoOp, external(BabaExternals.THROW_RESOLVE)),
                    ),
                )
            ),
        )

    fun shockwave(): Effect =
        sequence(
            setVarn(BabaVarns.LAST_SPECIAL, SPECIAL_SHOCKWAVE),
            whenever(
                shaking,
                wave(targetCentre, BabaShock.LENGTH_SHAKING, BabaShock.TELEGRAPH_TICKS),
                wave(targetCentre, BabaShock.LENGTH, BabaShock.TELEGRAPH_TICKS),
            ),
        )

    fun stomp(): Effect =
        sequence(
            setVarn(BabaVarns.UNDER, 0),
            wave(bossCentre, BabaShock.LENGTH_SHAKING, BabaShock.STOMP_WARNING_TICKS),
        )

    fun discharge(): Effect =
        sequence(
            setVarn(BabaVarns.LAST_SPECIAL, SPECIAL_DISCHARGE),
            anim(BabaSeqs.DISCHARGE),
            external(BabaExternals.BABOONS_SPAWN),
        )

    fun stompDue(): Condition = varnAtLeast(BabaVarns.UNDER, BabaShock.STOMP_TRIGGER_TICKS)

    private fun edgeTile(z: Int): TargetExpr.Single =
        customTile { npc, _ -> ToaRooms.of<BabaEncounter>(npc)?.edgeTile(z) ?: npc.coords }

    private fun fallTiers(build: (fall: Int, gfx: String) -> Effect): Effect =
        whenever(
            deeperPath,
            build(FALL_BASE - FALL_STEP * 2, BabaSpots.ROCK_FALL[2]),
            whenever(
                pathStarted,
                build(FALL_BASE - FALL_STEP, BabaSpots.ROCK_FALL[1]),
                build(FALL_BASE, BabaSpots.ROCK_FALL[0]),
            ),
        )

    private fun marker(at: TargetExpr.Single, rubble: Boolean): Effect = fallTiers { fall, gfx ->
        val landing =
            after(
                fall,
                whenever(
                    rolling,
                    Effect.NoOp,
                    hit {
                        target = playersOn(at)
                        delay = 1
                        damage(
                            roomDamage<BabaEncounter> { room, _, _ ->
                                room.hazardRoll(BabaRocks.IMPACT_DAMAGE, BabaRocks.IMPACT_SPREAD)
                            }
                        )
                        type(Typeless)
                        hazard()
                    },
                ),
            )
        val debris =
            if (rubble) {
                after(
                    fall,
                    whenever(rolling, Effect.NoOp, external(BabaExternals.RUBBLE_SPAWN, at = at)),
                )
            } else {
                Effect.NoOp
            }
        sequence(mapSpotanim(gfx, at, delay = BabaRocks.MARK_DELAY_CYCLES), landing, debris)
    }

    private fun wave(centre: (Npc, Player) -> CoordGrid, reach: Int, warning: Int): Effect =
        withTile(
            CENTRE,
            customTile(centre),
            rings(
                centre,
                reach,
                sequence(
                    lockMovement(),
                    external(BabaExternals.HOLD),
                    faceTile(tile(CENTRE)),
                    anim(BabaSeqs.MELEE),
                    *telegraph(reach),
                    wait(warning),
                    anim(BabaSeqs.MELEE),
                    *impact(reach),
                    wait(BabaShock.ATTACK_PAUSE),
                    unlockMovement(),
                ),
            ),
        )

    private fun rings(centre: (Npc, Player) -> CoordGrid, reach: Int, body: Effect): Effect =
        (0..reach).toList().foldRight(body) { dist, inner ->
            withTiles(
                RING + dist,
                customTiles(challengeArea) { npc, target, _ -> ring(centre(npc, target), dist, reach) },
                inner,
            )
        }

    private fun telegraph(reach: Int): Array<Effect> =
        (0..reach)
            .flatMap { ring ->
                val gfx = BabaSpots.SHOCK_TELEGRAPH[ring.coerceAtMost(BabaSpots.SHOCK_TELEGRAPH.lastIndex)]
                armOffsets(ring).map { (dx, dz) ->
                    mapSpotanim(
                        gfx,
                        offset(tile(CENTRE), dx, dz),
                        delay = ring * BabaShock.TELEGRAPH_STEP_CYCLES,
                    )
                }
            }
            .toTypedArray()

    private fun impact(reach: Int): Array<Effect> =
        (0..reach)
            .map { dist ->
                val base = BabaShock.BASE_DAMAGE * (100 - BabaShock.FALLOFF_PERCENT * dist) / 100
                onTiles(
                    bound(RING + dist),
                    sequence(
                        mapSpotanim(
                            BabaSpots.SHOCK_IMPACT,
                            CurrentTile,
                            delay = dist * BabaShock.IMPACT_STEP_CYCLES,
                        ),
                        hit {
                            target = playersOn(CurrentTile)
                            delay = 1
                            damage(
                                roomDamage<BabaEncounter> { room, _, _ ->
                                    room.hazardRoll(base, BabaShock.DAMAGE_SPREAD)
                                }
                            )
                            type(Typeless)
                            hazard()
                        },
                    ),
                )
            }
            .toTypedArray()

    private fun armOffsets(ring: Int): List<Pair<Int, Int>> =
        if (ring == 0) {
            listOf(0 to 0)
        } else {
            listOf(ring to 0, -ring to 0, 0 to ring, 0 to -ring)
        }

    private fun ring(centre: CoordGrid, dist: Int, reach: Int): List<CoordGrid> = buildList {
        for (dx in -dist..dist) {
            for (dz in -dist..dist) {
                if (max(abs(dx), abs(dz)) != dist) continue
                if (dist == reach && dx != 0 && dz != 0) continue
                add(centre.translate(dx, dz))
            }
        }
    }
}

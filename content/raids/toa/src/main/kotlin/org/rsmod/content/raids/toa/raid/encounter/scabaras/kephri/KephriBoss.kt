package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Centre
import org.rsmod.api.bosses.dsl.CurrentTarget
import org.rsmod.api.bosses.dsl.CurrentTile
import org.rsmod.api.bosses.dsl.CurrentTargetTile
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.BossSpecBuilder
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.customTile
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.mapSpotanim
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.nearestFreeTiles
import org.rsmod.api.bosses.dsl.nextAttackIn
import org.rsmod.api.bosses.dsl.offset
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.onTiles
import org.rsmod.api.bosses.dsl.playersOn
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.setVarn
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.tile
import org.rsmod.api.bosses.dsl.toward
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.dsl.withTile
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.challengeArea
import org.rsmod.content.raids.toa.raid.encounter.challengePlayers
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.raidAccuracy
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriAbilities.AUTO
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriAbilities.BOMBS
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriAbilities.DUNG
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriAbilities.EGGS
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriAbilities.FINAL_DUNG
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriAbilities.SPECIAL
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriPhases.FINAL
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriPhases.OPENING
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriPhases.SHIELD_2
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriPhases.SHIELD_3
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriPhases.STUN_1
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriPhases.STUN_2
import org.rsmod.content.raids.toa.raid.encounter.targetCondition
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class KephriBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = { ToaRooms.of<KephriEncounter>(npc)?.modifyKephriHit(npc, hit) },
        )
        deps.onRoomExternal<KephriEncounter>(PICK_DUNG) { room, _ -> room.dung.pick() }
        deps.onRoomExternal<KephriEncounter>(DUNG_PUSH) { room, ext ->
            room.dung.throwBack(ext.npc, ext.target)
        }
        deps.onRoomExternal<KephriEncounter>(EGGS_START) { room, ext -> room.startEggs(ext.npc) }
        deps.onRoomExternal<KephriEncounter>(SHADOW) { room, ext ->
            ext.tile?.let { room.shadow(it, ext.params as String) }
        }
    }

    override val spec: BossSpec =
        boss(KephriNpcs.SHIELDED, KephriNpcs.WEAK, KephriNpcs.ENRAGE) {
            ability(AUTO, whenever(fighting, auto()))
            ability(SPECIAL, whenever(eggsDue, run(EGGS), run(DUNG)))
            ability(
                EGGS,
                sequence(
                    setVarn(KephriVarns.EGGS_NEXT, 0),
                    anim(KephriSeqs.THROW),
                    external(EGGS_START),
                ),
            )
            ability(DUNG, dungStrike())
            ability(FINAL_DUNG, throwBack(everyone))
            ability(BOMBS, bombs())

            every(BOMB_INTERVAL, whenever(bombWindow, run(BOMBS)))

            shielded(OPENING, OPENING_AUTOS)
            stunned(STUN_1)
            shielded(SHIELD_2, SHIELDED_AUTOS, KephriNpcs.SHIELDED)
            stunned(STUN_2)
            shielded(SHIELD_3, SHIELDED_AUTOS, KephriNpcs.SHIELDED)
            shielded(FINAL, FINAL_AUTOS, KephriNpcs.ENRAGE)
        }

    private fun BossSpecBuilder.shielded(name: String, autos: IntRange, transmog: String? = null) {
        phase(name, transmog = transmog) {
            forceEveryAttacks(autos.first, autos.last, SPECIAL)
            rotationSelector { +then(AUTO) }
        }
    }

    private fun BossSpecBuilder.stunned(name: String) {
        phase(name, transmog = KephriNpcs.WEAK) {}
    }

    private fun auto(): Effect =
        sequence(
            anim(KephriSeqs.AUTO),
            withTile(
                APEX,
                toward(Centre, CurrentTargetTile, 1.0),
                sequence(
                    projectile(
                        spotanim = KephriSpots.AUTO_APEX,
                        from = Centre,
                        target = tile(APEX),
                        config = APEX_CONFIG,
                    ),
                    after(DROP_DELAY, withTile(AIM, CurrentTargetTile, drop())),
                ),
            ),
        )

    private fun drop(): Effect =
        (0 until MAX_FLIGHT_STEPS).reversed().fold(dropFlight(MAX_FLIGHT_STEPS)) { rest, steps ->
            whenever(flightSteps(steps), dropFlight(steps), rest)
        }

    private fun dropFlight(steps: Int): Effect {
        val travel = BASE_FLIGHT - FLIGHT_STEP * steps
        return sequence(
            external(SHADOW, SHADOWS[steps], at = tile(AIM)),
            projectile(
                spotanim = KephriSpots.AUTO_DROP,
                from = tile(APEX),
                target = tile(AIM),
                config =
                    ProjectileConfig.fixed(DROP_START_HEIGHT, IMPACT_HEIGHT, 0, travel, DROP_ANGLE),
            ),
            after(maxOf(1, travel / CLIENT_CYCLES_PER_TICK - 1), blast(tile(AIM))),
        )
    }

    private fun dungStrike(): Effect =
        sequence(
            setVarn(KephriVarns.EGGS_NEXT, 1),
            external(PICK_DUNG),
            onEach(
                challengePlayers,
                whenever(dungTarget, spotanim(KephriSpots.DUNG_INDICATOR, target = CurrentTarget)),
            ),
            after(INDICATOR_DELAY, whenever(dungPending, throwBack(dungTarget))),
            nextAttackIn(DUNG_ATTACK_GAP),
        )

    private fun throwBack(who: Condition): Effect =
        sequence(
            anim(KephriSeqs.THROW),
            onEach(
                challengePlayers,
                whenever(
                    who,
                    sequence(message(THROWN_TEXT), external(DUNG_PUSH)),
                ),
            ),
        )

    private fun bombs(): Effect = onEach(challengePlayers, whenever(firstOnTile, bomb()))

    private fun bomb(): Effect =
        withTile(
            BOMB,
            CurrentTargetTile,
            sequence(
                projectile(
                    spotanim = KephriSpots.BOMB,
                    from = nearestCorner,
                    target = tile(BOMB),
                    config =
                        ProjectileConfig.fixed(
                            BOMB_START_HEIGHT,
                            IMPACT_HEIGHT,
                            0,
                            BOMB_FLIGHT,
                            DROP_ANGLE,
                        ),
                ),
                after(
                    BOMB_FLIGHT / CLIENT_CYCLES_PER_TICK - 1,
                    sequence(
                        mapSpotanim(KephriSpots.BOMB_EXPLODE, tile(BOMB), delay = EXPLOSION_DELAY),
                        blast(tile(BOMB)),
                    ),
                ),
            ),
        )

    private fun blast(centre: TargetExpr.Single): Effect =
        whenever(
            aerial,
            sequence(
                blastAt(centre, AERIAL_MAX_HIT),
                onTiles(
                    nearestFreeTiles(surround(centre), challengeArea, 0),
                    blastAt(CurrentTile, AERIAL_MAX_HIT),
                ),
            ),
            blastAt(centre, MAX_HIT),
        )

    private fun blastAt(at: TargetExpr.Single, maxHit: Int): Effect =
        sequence(
            oneOf(
                KephriSpots.EXPLOSIONS.map {
                    mapSpotanim(it, at, height = IMPACT_HEIGHT, delay = EXPLOSION_DELAY)
                }
            ),
            hit {
                target = playersOn(at)
                delay = 1
                damage(raidAccuracy(maxHit, Magic))
                type(Magic)
                penetration(PRAYER_PENETRATION)
            },
        )

    private fun surround(centre: TargetExpr.Single): List<TargetExpr.Single> =
        (-1..1)
            .flatMap { dx -> (-1..1).map { dz -> dx to dz } }
            .filter { it != 0 to 0 }
            .map { (dx, dz) -> offset(centre, dx, dz) }

    private companion object {
        const val PICK_DUNG = "kephri.pick_dung"
        const val DUNG_PUSH = "kephri.dung_push"
        const val EGGS_START = "kephri.eggs_start"
        
        val OPENING_AUTOS = 3..3
        val SHIELDED_AUTOS = 4..5
        val FINAL_AUTOS = 2..2
        const val THROWN_TEXT = "<col=ff289d>Kephri throws you back!</col>"
        const val INDICATOR_DELAY = 5
        const val DUNG_ATTACK_GAP = 13

        const val AIM = "aim"
        const val APEX = "apex"
        const val BOMB = "bomb"

        const val MAX_HIT = 24
        const val AERIAL_MAX_HIT = 30
        const val PRAYER_PENETRATION = 33
        const val SHADOW = "kephri.shadow"
        val SHADOWS =
            listOf(
                "spotanim.gargboss_debris_shadow_120",
                "spotanim.gargboss_debris_shadow_90",
                "spotanim.gargboss_debris_shadow_60",
                "spotanim.gargboss_debris_shadow_30",
            )
        const val CLIENT_CYCLES_PER_TICK = 30
        const val EXPLOSION_DELAY = 30
        const val IMPACT_HEIGHT = 38

        const val DROP_DELAY = 3
        const val BASE_FLIGHT = 120
        const val FLIGHT_STEP = 30
        const val MAX_FLIGHT_STEPS = 3
        const val DROP_START_HEIGHT = 1000
        const val DROP_ANGLE = 115

        const val BOMB_INTERVAL = 12
        const val BOMB_FLIGHT = 120
        const val BOMB_START_HEIGHT = 450

        val APEX_CONFIG = ProjectileConfig.fixed(700, 1000, 39, 51, 50)

        val fighting = roomCondition<KephriEncounter> { room, npc -> room.fighting(npc) }
        val eggsDue = roomCondition<KephriEncounter> { room, npc -> room.eggsDue(npc) }
        val dungPending = roomCondition<KephriEncounter> { room, _ -> room.dung.pending }
        val dungTarget = targetCondition<KephriEncounter> { room, player -> player in room.dung.targets }
        val everyone = targetCondition<KephriEncounter> { _, _ -> true }
        val bombWindow = roomCondition<KephriEncounter> { room, _ -> room.bombWindow() }
        val aerial =
            roomCondition<KephriEncounter> { room, _ ->
                room.raid.isActive(ToaInvocationKey.AerialAssault)
            }
        val firstOnTile =
            targetCondition<KephriEncounter> { room, player ->
                room.targets().first { it.coords == player.coords } === player
            }

        fun flightSteps(steps: Int) =
            roomCondition<KephriEncounter> { room, _ ->
                (room.pathLevel / 2).coerceIn(0, MAX_FLIGHT_STEPS) == steps
            }

        val nearestCorner =
            customTile { npc, target ->
                val room = ToaRooms.roomOf(npc)
                val min = room?.room?.challengeMin
                val max = room?.room?.challengeMax
                if (room == null || min == null || max == null) {
                    npc.coords
                } else {
                    listOf(
                            CoordGrid(min.x, min.z, min.level),
                            CoordGrid(max.x, min.z, min.level),
                            CoordGrid(max.x, max.z, min.level),
                            CoordGrid(min.x, max.z, min.level),
                        )
                        .map(room::coords)
                        .minBy { corner -> corner.chebyshevDistance(target.coords) }
                }
            }
    }
}

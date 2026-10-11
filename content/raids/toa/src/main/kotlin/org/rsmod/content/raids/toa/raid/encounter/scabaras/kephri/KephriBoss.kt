package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Centre
import org.rsmod.api.bosses.dsl.CurrentTargetTile
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.customTile
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.nextAttackIn
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.CurrentTarget
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.mapSpotanim
import org.rsmod.api.bosses.dsl.offset
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.playersOn
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
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
import org.rsmod.content.raids.toa.raid.encounter.challengePlayers
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.raidAccuracy
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
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
        deps.onRoomExternal<KephriEncounter>(COUNT_AUTO) { room, _ -> room.countAuto() }
        deps.onRoomExternal<KephriEncounter>(PICK_SPECIAL) { room, _ -> room.pickSpecial() }
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
            ability(
                SPECIAL,
                sequence(external(PICK_SPECIAL), whenever(eggsPicked, run(EGGS), run(KephriAbilities.DUNG))),
            )
            ability(EGGS, sequence(anim(KephriSeqs.THROW), external(EGGS_START)))
            ability(KephriAbilities.DUNG, dungStrike())
            ability(KephriAbilities.FINAL_DUNG, throwBack(everyone))
            ability(KephriAbilities.BOMBS, bombs())

            every(BOMB_INTERVAL, whenever(bombWindow, run(KephriAbilities.BOMBS)))

            phase(MAIN_PHASE) {
                forceWhen(specialDue, SPECIAL)
                rotationSelector { +then(AUTO) }
            }
        }

    private fun auto(): Effect =
        sequence(
            external(COUNT_AUTO),
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
        whenever(
            flightSteps(0),
            dropFlight(0),
            whenever(
                flightSteps(1),
                dropFlight(1),
                whenever(flightSteps(2), dropFlight(2), dropFlight(3)),
            ),
        )

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
                external(SHADOW, SHADOWS[0], at = tile(BOMB)),
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

    private fun blast(centre: TargetExpr.Single): Effect {
        val tiles = area(centre).map { blastAt(it) }
        return whenever(aerial, sequence(*tiles.toTypedArray()), blastAt(centre))
    }

    private fun blastAt(at: TargetExpr.Single): Effect =
        sequence(
            oneOf(
                KephriSpots.EXPLOSIONS.map {
                    mapSpotanim(it, at, height = IMPACT_HEIGHT, delay = EXPLOSION_DELAY)
                }
            ),
            hit {
                target = playersOn(at)
                delay = 1
                damage(raidAccuracy(MAX_HIT, Magic))
                type(Magic)
                penetration(PRAYER_PENETRATION)
            },
        )

    private fun area(centre: TargetExpr.Single): List<TargetExpr.Single> =
        (-1..1).flatMap { dx -> (-1..1).map { dz -> offset(centre, dx, dz) } }

    private companion object {
        const val MAIN_PHASE = "main"
        const val AUTO = "auto"
        const val SPECIAL = "special"
        const val COUNT_AUTO = "kephri.count_auto"
        const val PICK_SPECIAL = "kephri.pick_special"
        const val PICK_DUNG = "kephri.pick_dung"
        const val DUNG_PUSH = "kephri.dung_push"
        const val EGGS_START = "kephri.eggs_start"
        const val EGGS = "eggs"
        const val THROWN_TEXT = "<col=ff289d>Kephri throws you back!</col>"
        const val INDICATOR_DELAY = 5
        const val DUNG_ATTACK_GAP = 13

        const val AIM = "aim"
        const val APEX = "apex"
        const val BOMB = "bomb"

        const val MAX_HIT = 24
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

        val CORNERS =
            listOf(
                CoordGrid(3543, 5416, 0),
                CoordGrid(3559, 5416, 0),
                CoordGrid(3559, 5400, 0),
                CoordGrid(3543, 5400, 0),
            )

        val fighting = roomCondition<KephriEncounter> { room, npc -> room.fighting(npc) }
        val specialDue =
            roomCondition<KephriEncounter> { room, npc -> room.fighting(npc) && room.specialDue }
        val eggsPicked = roomCondition<KephriEncounter> { room, _ -> room.eggsPicked }
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
                room?.let {
                    CORNERS.map(it::coords).minBy { corner ->
                        corner.chebyshevDistance(target.coords)
                    }
                } ?: npc.coords
            }
    }
}

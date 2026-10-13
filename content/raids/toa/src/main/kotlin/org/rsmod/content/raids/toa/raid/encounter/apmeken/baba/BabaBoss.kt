package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.HpBelow
import org.rsmod.api.bosses.dsl.InPhase
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.PhaseBuilder
import org.rsmod.api.bosses.dsl.addVarn
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.customTile
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.hitStyle
import org.rsmod.api.bosses.dsl.interrupt
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.nextAttackIn
import org.rsmod.api.bosses.dsl.not
import org.rsmod.api.bosses.dsl.offset
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.setVarn
import org.rsmod.api.bosses.dsl.teleport
import org.rsmod.api.bosses.dsl.transitionTo
import org.rsmod.api.bosses.dsl.transmog
import org.rsmod.api.bosses.dsl.varnAtLeast
import org.rsmod.api.bosses.dsl.varnExpired
import org.rsmod.api.bosses.dsl.varnIs
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.dsl.whenever
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
import org.rsmod.content.raids.toa.raid.encounter.eachTarget
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.raidAccuracy
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.content.raids.toa.raid.encounter.targetCondition
import org.rsmod.plugin.scripts.ScriptContext

class BabaBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = { ToaRooms.of<BabaEncounter>(npc)?.modifyBabaHit(npc, hit) },
        )
        deps.onRoomExternal<BabaEncounter>(BabaExternals.RETARGET) { room, ext ->
            room.retarget(ext.npc)
        }
        deps.onRoomExternal<BabaEncounter>(BabaExternals.FLYBACK) { room, ext ->
            room.boulders.flyback(ext.target)
        }
        deps.onRoomExternal<BabaEncounter>(BabaExternals.BOULDER_ROW) { room, ext ->
            room.boulderRow(ext.npc)
        }
        deps.onRoomExternal<BabaEncounter>(BabaExternals.ROLL_RESET) { room, ext ->
            room.rollReset(ext.npc)
        }
        deps.onRoomExternal<BabaEncounter>(BabaExternals.THROW_RESOLVE) { room, ext ->
            room.rockfall.resolveThrow(ext.target)
        }
        deps.onRoomExternal<BabaEncounter>(BabaExternals.RUBBLE_SPAWN) { room, ext ->
            ext.tile?.let(room.rockfall::spawnRubble)
        }
        deps.onRoomExternal<BabaEncounter>(BabaExternals.BABOONS_SPAWN) { room, ext ->
            room.baboons.discharge(ext.npc)
        }
    }

    override val spec: BossSpec =
        boss(BabaNpcs.BABA, BabaNpcs.DIGGING, BabaNpcs.COFFIN) {
            ability(BabaAbilities.AUTO, whenever(fighting, auto()))
            ability(BabaAbilities.ROCK_THROW, BabaSpecials.rockThrow())
            ability(BabaAbilities.ROLL_ENTRY, rollEntry())
            ability(BabaAbilities.ROLL_END, rollEnd())
            val rollFlag =
                ability(
                    BabaAbilities.ROLL_FLAG,
                    sequence(
                        setVarn(BabaVarns.ROLL_EXIT, 1),
                        after(1, run(BabaAbilities.ROLL_END)),
                    ),
                )
            ability(BabaAbilities.SPECIAL, BabaSpecials.special())
            ability(BabaAbilities.ROCKFALL, BabaSpecials.rockfall())
            ability(BabaAbilities.SHOCKWAVE, BabaSpecials.shockwave())
            ability(BabaAbilities.STOMP, BabaSpecials.stomp())
            ability(BabaAbilities.DISCHARGE, BabaSpecials.discharge())

            every(1, whenever(underBoss and !rolling, addVarn(BabaVarns.UNDER, 1), setVarn(BabaVarns.UNDER, 0)))

            incoming { rule(rolling and digging and hitStyle(Melee) and fromTop) { run(rollFlag) } }

            triggers {
                on(HpBelow(FIRST_ROLL_HP) and InPhase(BabaPhases.NORMAL)) runs
                    run(BabaAbilities.ROLL_ENTRY)
                on(HpBelow(SECOND_ROLL_HP) and InPhase(BabaPhases.NORMAL_2)) runs
                    run(BabaAbilities.ROLL_ENTRY)
            }

            phase(BabaPhases.NORMAL) { combat() }
            phase(BabaPhases.ROLLING, lockMovement = true) {
                every(1, whenever(rollOver, run(BabaAbilities.ROLL_END)))
            }
            phase(BabaPhases.NORMAL_2, transmog = BabaNpcs.BABA) { combat() }
            phase(BabaPhases.FINAL, transmog = BabaNpcs.COFFIN) { combat() }
        }

    private fun PhaseBuilder.combat() {
        forceWhen(varnExpired(BabaVarns.THROW_AT), BabaAbilities.ROCK_THROW)
        forceWhen(BabaSpecials.stompDue(), BabaAbilities.STOMP)
        rotationSelector { +then(BabaAbilities.AUTO) }
    }

    private fun auto(): Effect =
        sequence(
            anim(BabaSeqs.MELEE),
            hit {
                damage(raidAccuracy(MELEE_MAX_HIT, Melee, MeleeAttackType.Crush))
                type(Melee)
                delay = MELEE_HIT_DELAY
            },
            external(BabaExternals.RETARGET),
            addVarn(BabaVarns.AUTOS, 1),
            whenever(specialDue, BabaSpecials.specialSlot()),
        )

    private fun rollEntry(): Effect =
        sequence(
            interrupt(),
            setVarn(BabaVarns.THROW_AT, 0),
            setVarn(BabaVarns.ROLL_EXIT, 0),
            setVarn(BabaVarns.ROWS, 0),
            external(BabaExternals.ROLL_RESET),
            transitionTo(BabaPhases.ROLLING),
            wait(JUMP_DELAY),
            anim(BabaSeqs.JUMP),
            teleport(spawnTile),
            wait(SCREAM_DELAY),
            eachTarget(message(BabaRolling.SCREAM_TEXT)),
            transmog(BabaNpcs.DIGGING, Int.MAX_VALUE),
            eachTarget(external(BabaExternals.FLYBACK)),
            wait(FIRST_CAST_DELAY),
            whenever(
                boulderDash,
                castLoop(BabaRolling.CASTS, BabaRolling.BOULDER_DASH_GAP),
                castLoop(BabaRolling.CASTS, BabaRolling.CAST_GAP),
            ),
            wait(BabaRolling.CAST_GAP),
            run(BabaAbilities.ROLL_END),
        )

    private fun castLoop(casts: Int, gap: Int): Effect =
        repeat(casts, gap, whenever(rollOver, Effect.NoOp, cast()))

    private fun cast(): Effect =
        sequence(
            anim(BabaSeqs.CAST),
            *LANE_OFFSETS.map { dz ->
                    projectile(
                        spotanim = BabaSpots.BALLS_TRAVEL,
                        from = offset(spawnTile, CAST_FROM_DX, CAST_FROM_DZ),
                        target = offset(spawnTile, 0, dz),
                        config = BALLS_CONFIG,
                    )
                }
                .toTypedArray(),
            after(BabaRolling.ROW_DELAY, external(BabaExternals.BOULDER_ROW)),
        )

    private fun rollEnd(): Effect =
        whenever(
            rolling,
            sequence(
                interrupt(),
                anim(BabaSeqs.END),
                addVarn(BabaVarns.ROLLS, 1),
                setVarn(BabaVarns.ROWS, 0),
                setVarn(BabaVarns.ROLL_EXIT, 0),
                whenever(
                    varnAtLeast(BabaVarns.ROLLS, 2),
                    transitionTo(BabaPhases.FINAL),
                    transitionTo(BabaPhases.NORMAL_2),
                ),
                nextAttackIn(END_ATTACK_DELAY),
            ),
        )

    private companion object {
        const val MELEE_MAX_HIT = 24
        const val MELEE_HIT_DELAY = 1
        const val FIRST_ROLL_HP = 0.66
        const val SECOND_ROLL_HP = 0.33
        const val JUMP_DELAY = 2
        const val SCREAM_DELAY = 2
        const val FIRST_CAST_DELAY = 2
        const val END_ATTACK_DELAY = 3
        const val CAST_FROM_DX = 1
        const val CAST_FROM_DZ = 3

        val LANE_OFFSETS = listOf(7, 4, 1, -2, -5)
        val BALLS_CONFIG = ProjectileConfig.fixed(200, 20, 30, 60, 35)

        val spawnTile: TargetExpr.Single =
            customTile { npc, _ ->
                ToaRooms.roomOf(npc)?.coords(BabaCoords.BABA) ?: npc.coords
            }

        val rolling = InPhase(BabaPhases.ROLLING)
        val digging = Condition.Custom { npc, _ -> npc.isVisType(BabaNpcs.DIGGING) }
        val boulderDash =
            roomCondition<BabaEncounter> { room, _ ->
                room.raid.isActive(ToaInvocationKey.Boulderdash)
            }
        val rollOver = varnIs(BabaVarns.ROLL_EXIT, 1)
        val fighting = roomCondition<BabaEncounter> { room, npc -> room.fighting(npc) }
        val underBoss = roomCondition<BabaEncounter> { room, npc -> room.underBoss(npc) }
        val fromTop =
            targetCondition<BabaEncounter> { room, player ->
                room.staticCoords(player.coords).x >= BabaRolling.TOP_ZONE_X
            }
        val specialDue =
            Condition.Custom { npc, _ ->
                val autos = npc.vars[BabaVarns.AUTOS]
                autos == 1 || autos % BabaRocks.SPECIAL_AUTOS == 0
            }
    }
}

package org.rsmod.content.raids.toa.raid.encounter.wardens

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.interrupt
import org.rsmod.api.bosses.dsl.nextAttackIn
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.transitionTo
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.plugin.scripts.ScriptContext

internal object WardenFinalAbilities {
    const val SIPHONS = "siphons"
    const val RESOLVE = "resolve"
    const val LAUNCH = "wardens.siphon_launch"
    const val RESOLVED = "wardens.siphon_resolved"
}

class WardenFinalBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = { ToaRooms.of<WardensSecondEncounter>(npc)?.modifyWardenHit(npc, hit) },
        )
        for (slam in WardenSlam.entries) {
            deps.onRoomExternal<WardensSecondEncounter>(slam.ability) { room, _ ->
                room.slams.fire(slam)
            }
        }
        deps.onRoomExternal<WardensSecondEncounter>(WardenFinalAbilities.LAUNCH) { room, ext ->
            room.siphons.launch(ext.npc)
        }
        deps.onRoomExternal<WardensSecondEncounter>(WardenFinalAbilities.RESOLVED) { room, _ ->
            room.siphons.finish()
        }
    }

    override val spec: BossSpec =
        boss(*WardenGod.entries.flatMap { listOf(it.phaseThree, it.phaseThreeCharging) }.toTypedArray()) {
            for (slam in WardenSlam.entries) ability(slam.ability, slam(slam))
            ability(
                WardenFinalAbilities.SIPHONS,
                sequence(
                    interrupt(),
                    anim(CHARGE_SEQ),
                    external(WardenFinalAbilities.LAUNCH),
                    transitionTo(CHARGING_PHASE),
                ),
            )
            ability(
                WardenFinalAbilities.RESOLVE,
                after(
                    RESOLVE_DELAY,
                    sequence(
                        anim(STAGGER_SEQ),
                        external(WardenFinalAbilities.RESOLVED),
                        transitionTo(SLAM_PHASE),
                        nextAttackIn(RESUME_DELAY),
                    ),
                ),
            )

            phase(SLAM_PHASE) {
                rotationSelector {
                    +then(WardenSlam.EAST.ability)
                    +then(WardenSlam.WEST.ability)
                    +then(WardenSlam.BOTH.ability)
                }
                forceWhen(siphonDue, WardenFinalAbilities.SIPHONS)
            }
            phase(CHARGING_PHASE, lockMovement = true) {}
        }

    private fun slam(slam: WardenSlam): Effect =
        sequence(
            whenever(fastSlams, anim(slam.fastSeq), anim(slam.seq)),
            external(slam.ability),
        )

    private companion object {
        const val SLAM_PHASE = "slams"
        const val CHARGING_PHASE = "charging"
        const val CHARGE_SEQ = "seq.npc_wardens_charge01"
        const val STAGGER_SEQ = "seq.npc_wardens_stagger01"
        const val RESOLVE_DELAY = 1
        const val RESUME_DELAY = 9

        val siphonDue = roomCondition<WardensSecondEncounter> { room, _ -> room.siphonDue }
        val fastSlams = roomCondition<WardensSecondEncounter> { room, _ -> room.fastSlams }
    }
}

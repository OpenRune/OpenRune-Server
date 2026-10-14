package org.rsmod.content.raids.toa.raid.encounter.wardens

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.plugin.scripts.ScriptContext

class WardenFinalBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        for (slam in WardenSlam.entries) {
            deps.onRoomExternal<WardensSecondEncounter>(slam.ability) { room, _ ->
                room.slams.fire(slam)
            }
        }
    }

    override val spec: BossSpec =
        boss(*WardenGod.entries.map { it.phaseThree }.toTypedArray()) {
            for (slam in WardenSlam.entries) ability(slam.ability, slam(slam))

            phase(SLAM_PHASE) {
                rotationSelector {
                    +then(WardenSlam.EAST.ability)
                    +then(WardenSlam.WEST.ability)
                    +then(WardenSlam.BOTH.ability)
                }
            }
        }

    private fun slam(slam: WardenSlam): Effect =
        sequence(
            whenever(fastSlams, anim(slam.fastSeq), anim(slam.seq)),
            external(slam.ability),
        )

    private companion object {
        const val SLAM_PHASE = "slams"

        val fastSlams = roomCondition<WardensSecondEncounter> { room, _ -> room.fastSlams }
    }
}

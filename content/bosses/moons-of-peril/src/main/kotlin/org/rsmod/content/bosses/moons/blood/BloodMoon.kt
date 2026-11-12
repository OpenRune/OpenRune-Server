package org.rsmod.content.bosses.moons.blood

import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.focusPhase
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.content.bosses.moons.MoonsExternals

class BloodMoon @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec: BossSpec =
        boss(Moon.Blood.npc) {
            ambient()
            stats(attackRate = STANDARD_ATTACK_RATE)

            val subdued = ability("subdued", external(MoonsExternals.SUBDUED))
            personalHp(
                Moon.MAX_HITPOINTS,
                hpVarbit = "varbit.hpbar_hud_hp",
                onDeath = subdued,
                headbar = "headbar.blood_120",
            )

            val burn = onEach(playersIn(ARENA), external(MoonsExternals.BURN))
            every(
                CONDITION_DECAY_TICKS,
                onEach(playersIn(ARENA), addPlayerVar(MoonsFights.CONDITION, -CONDITION_DECAY)),
            )

            val jaguars = ability("jaguars", BloodJaguars.timeline)
            val rain = ability("rain", BloodRain.timeline)

            phase("jaguars", lockMovement = true, exitAfter = JAGUARS_TICKS, nextPhase = "focus_1") {
                entry = jaguars.name
            }
            focusPhase(Moon.Blood, 1, "focus_2", burn)
            focusPhase(Moon.Blood, 2, "focus_3", burn)
            focusPhase(Moon.Blood, 3, "rain", burn)
            phase("rain", lockMovement = true, exitAfter = RAIN_TICKS, nextPhase = "focus_5") {
                entry = rain.name
            }
            focusPhase(Moon.Blood, 5, "focus_6", burn)
            focusPhase(Moon.Blood, 6, "focus_7", burn)
            focusPhase(Moon.Blood, 7, "jaguars", burn)
        }

    private companion object {
        private const val STANDARD_ATTACK_RATE = 6
        private const val CONDITION_DECAY_TICKS = 5
        private const val CONDITION_DECAY = 2
        private const val JAGUARS_TICKS = 53
        private const val RAIN_TICKS = 31
        private val ARENA = area(spawnTile(-12, -14), spawnTile(18, 15))
    }
}

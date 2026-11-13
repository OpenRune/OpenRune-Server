package org.rsmod.content.bosses.moons.blue

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals
import org.rsmod.content.bosses.moons.arena
import org.rsmod.content.bosses.moons.focusPhase
import org.rsmod.plugin.scripts.ScriptContext

class BlueMoon @Inject constructor(deps: BossDeps, private val blue: BlueFights) :
    BossPluginScript(deps) {

    override val spec: BossSpec =
        boss(Moon.Blue.npc) {
            ambient()
            stats(attackRate = STANDARD_ATTACK_RATE)

            val arena = Moon.Blue.arena()
            val subdued = ability("subdued", external(MoonsExternals.SUBDUED))
            personalHp(
                Moon.MAX_HITPOINTS,
                hpVarbit = "varbit.hpbar_hud_hp",
                onDeath = subdued,
                headbar = "headbar.ice_120",
            )

            val burn = onEach(playersIn(arena), external(MoonsExternals.BURN))
            every(
                COLD_DECAY_TICKS,
                onEach(playersIn(arena), external(MoonsExternals.COLD_DECAY)),
            )

            val storm = ability("storm", BlueStorm.timeline)
            val freeze = ability("freeze", BlueFreeze.timeline)

            phase("storm", lockMovement = true, exitAfter = STORM_TICKS, nextPhase = "focus_1") {
                entry = storm.name
            }
            focusPhase(
                Moon.Blue,
                1,
                "focus_2",
                burn,
                ticks = FIRST_FOCUS_TICKS,
                firstAttack = FIRST_FOCUS_ATTACK,
                circleAt = FIRST_FOCUS_CIRCLE,
                vulnerableAt = FIRST_FOCUS_VULNERABLE,
            )
            focusPhase(Moon.Blue, 2, "focus_3", burn)
            focusPhase(Moon.Blue, 3, "freeze", burn)
            phase("freeze", lockMovement = true, exitAfter = FREEZE_TICKS, nextPhase = "focus_5") {
                entry = freeze.name
            }
            focusPhase(Moon.Blue, 5, "focus_6", burn, spawnCircle = false)
            focusPhase(Moon.Blue, 6, "focus_7", burn)
            focusPhase(Moon.Blue, 7, "storm", burn)
        }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { blue.coldLock(this) })
    }

    private companion object {
        private const val STANDARD_ATTACK_RATE = 6
        private const val COLD_DECAY_TICKS = 5
        private const val STORM_TICKS = 50
        private const val FREEZE_TICKS = 58
        private const val FIRST_FOCUS_TICKS = 24
        private const val FIRST_FOCUS_ATTACK = 15
        private const val FIRST_FOCUS_CIRCLE = 6
        private const val FIRST_FOCUS_VULNERABLE = 6
    }
}

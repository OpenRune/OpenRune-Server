package org.rsmod.content.bosses.moons.eclipse

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsExternals
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.content.bosses.moons.arena
import org.rsmod.content.bosses.moons.focusPhase
import org.rsmod.plugin.scripts.ScriptContext

class EclipseMoon @Inject constructor(deps: BossDeps, private val eclipse: EclipseFights) :
    BossPluginScript(deps) {

    override val spec: BossSpec =
        boss(Moon.Eclipse.npc) {
            ambient()
            stats(attackRate = STANDARD_ATTACK_RATE)

            val arena = Moon.Eclipse.arena()
            val subdued = ability("subdued", external(MoonsExternals.SUBDUED))
            personalHp(
                Moon.MAX_HITPOINTS,
                hpVarbit = "varbit.hpbar_hud_hp",
                onDeath = subdued,
                headbar = "headbar.heat_120",
            )

            val burn = onEach(playersIn(arena), external(MoonsExternals.BURN))
            every(
                CONDITION_DECAY_TICKS,
                onEach(playersIn(arena), addPlayerVar(MoonsFights.CONDITION, -CONDITION_DECAY)),
            )

            val rays = ability("rays", EclipseRays.timeline)
            val clones = ability("clones", EclipseClones.timeline)

            phase("rays", lockMovement = true, exitAfter = RAYS_TICKS, nextPhase = "focus_1") {
                entry = rays.name
            }
            focusPhase(Moon.Eclipse, 1, "focus_2", burn, spawnCircle = false)
            focusPhase(Moon.Eclipse, 2, "focus_3", burn)
            focusPhase(Moon.Eclipse, 3, "clones", burn)
            phase("clones", lockMovement = true, exitAfter = CLONES_TICKS, nextPhase = "focus_5") {
                entry = clones.name
            }
            focusPhase(Moon.Eclipse, 5, "focus_6", burn, ticks = LONG_FOCUS_TICKS, firstAttack = LONG_FOCUS_ATTACK)
            focusPhase(Moon.Eclipse, 6, "focus_7", burn)
            focusPhase(Moon.Eclipse, 7, "rays", burn)
        }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { eclipse.glance(this) })
    }

    private companion object {
        private const val STANDARD_ATTACK_RATE = 6
        private const val CONDITION_DECAY_TICKS = 5
        private const val CONDITION_DECAY = 2
        private const val RAYS_TICKS = 55
        private const val CLONES_TICKS = 55
        private const val LONG_FOCUS_TICKS = 17
        private const val LONG_FOCUS_ATTACK = 8
    }
}

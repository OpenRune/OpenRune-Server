package org.rsmod.api.bosses.runtime

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.validation.SpecValidator
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

object BossCombat {

    fun register(
        ctx: ScriptContext,
        spec: BossSpec,
        deps: BossDeps,
        onLethal: ((Npc) -> Unit)? = null,
        onModifyHit: (NpcHitEvents.Modify.() -> Unit)? = null,
    ) {
        val errors = SpecValidator.validate(spec)
        if (errors.isNotEmpty()) {
            val msg = errors.joinToString("\n") { "  - ${it.message}" }
            error("Boss spec validation failed for '${spec.npcType}':\n$msg")
        }

        val npcId = spec.npcType.asRSCM(RSCMType.NPC)
        val npcType = ServerCacheManager.getNpc(npcId)
            ?: error("Boss NPC type not found: ${spec.npcType}")

        deps.encounterRegistry.register(npcId, spec)

        with(ctx) {
            onAiOpPlayer2(npcType) { runCombatTick(it.target, spec, deps) }
            onAiApPlayer2(npcType) { runCombatTick(it.target, spec, deps) }
            onModifyNpcHit(npcType) {
                val encounter = deps.encounterRegistry.of(npc)
                hit.damage =
                    if (encounter.invulnerable) 0 else (hit.damage * encounter.damageScale).toInt()
                onModifyHit?.invoke(this)
                if (onLethal != null && !encounter.lethalHandled && npc.hitpoints - hit.damage <= 0) {
                    encounter.lethalHandled = true
                    onLethal(npc)
                }
            }
        }
    }

    private suspend fun StandardNpcAccess.runCombatTick(
        target: Player,
        spec: BossSpec,
        deps: BossDeps,
    ) {
        val encounter = deps.encounterRegistry.of(npc)
        val phase = encounter.currentPhase ?: return
        val tick = deps.mapClock.cycle

        checkAutoTransitions(encounter, tick, spec)

        val ticksSinceLastAttack = tick - encounter.lastAbilityTick
        if (ticksSinceLastAttack < spec.stats.attackRate) return

        val abilityName = encounter.selectAbility(phase.selector, tick, target) ?: return
        val effect = spec.abilities[abilityName] ?: return

        encounter.lastAbilityTick = tick

        val interpreter = EffectInterpreter(npc, target, spec, encounter, deps)
        interpreter.run(this, effect)
    }

    private fun checkAutoTransitions(encounter: BossEncounter, tick: Int, spec: BossSpec) {
        for ((name, phase) in spec.phases) {
            if (name == encounter.currentPhaseName) continue
            val entryHp = phase.entryHp ?: continue
            val hpFraction = encounter.npc.hitpoints.toDouble() /
                encounter.npc.baseHitpointsLvl.coerceAtLeast(1)
            if (hpFraction <= entryHp) {
                encounter.transitionTo(name, tick)
                return
            }
        }
    }
}

package org.rsmod.api.mechanics.toxins.impl

import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType

public object NpcPoison {
    private val NoopModifier = NpcHitModifier {}

    public fun isPoisoned(npc: Npc): Boolean = npc.vars["varn.poison_severity"] > 0

    public fun isImmune(npc: Npc): Boolean =
        (npc.visType.paramOrNull(params.poison_immunity) ?: 0) > 0

    public fun tryPoison(npc: Npc, severity: Int): Boolean {
        if (severity <= 0 || isImmune(npc)) {
            return false
        }
        val current = npc.vars["varn.poison_severity"]
        if (current > 0) {
            val currentDamage = PlayerPoison.damageForSeverity(current)
            val incomingDamage = PlayerPoison.damageForSeverity(severity)
            if (incomingDamage < currentDamage) return false
            if (incomingDamage == currentDamage && severity <= current) return false
        }
        queuePoisonHit(npc, PlayerPoison.damageForSeverity(severity))
        setSeverity(npc, severity - 1)
        return true
    }

    public fun clear(npc: Npc) {
        npc.vars["varn.poison_severity"] = 0
        npc.clearTimer("timer.npc_poison")
    }

    public fun onPoisonTimerTick(npc: Npc) {
        val severity = npc.vars["varn.poison_severity"]
        if (severity <= 0 || npc.hitpoints <= 0) {
            clear(npc)
            return
        }
        queuePoisonHit(npc, PlayerPoison.damageForSeverity(severity))
        setSeverity(npc, severity - 1)
    }

    private fun setSeverity(npc: Npc, severity: Int) {
        if (severity <= 0) {
            clear(npc)
            return
        }
        npc.vars["varn.poison_severity"] = severity
        npc.timer("timer.npc_poison", PlayerPoison.TICK_INTERVAL)
    }

    private fun queuePoisonHit(npc: Npc, damage: Int) {
        npc.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            modifier = NoopModifier,
            hitmark = hitmark_groups.poison_damage,
        )
    }
}

package org.rsmod.api.mechanics.toxins.impl

import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType

public object NpcVenom {
    public const val STRIKES: String = "varn.npc_venom_strikes"
    public const val TIMER: String = "timer.npc_venom"
    private val noModifier: NpcHitModifier = NpcHitModifier {}

    public fun isEnvenomed(npc: Npc): Boolean = npc.vars[STRIKES] > 0

    public fun tryVenom(npc: Npc): Boolean {
        if (npc.hitpoints <= 0 || NpcPoison.isImmune(npc) || isEnvenomed(npc)) return false
        if ((npc.visType.paramOrNull(params.venom_immunity) ?: 0) > 0) {
            return NpcPoison.tryPoison(npc, PlayerPoison.severityForInitialDamage(6))
        }
        NpcPoison.clear(npc)
        npc.vars[STRIKES] = 1
        npc.timer(TIMER, PlayerVenom.TICK_INTERVAL)
        return true
    }

    public fun clear(npc: Npc) {
        npc.vars[STRIKES] = 0
        npc.clearTimer(TIMER)
    }

    public fun tick(npc: Npc) {
        val strike = npc.vars[STRIKES]
        if (strike == 0 || npc.hitpoints <= 0) { clear(npc); return }
        npc.queueHit(delay = 1, type = HitType.Typeless, damage = PlayerVenom.damageForStrikeIndex(strike - 1), modifier = noModifier, hitmark = hitmark_groups.venom)
        npc.vars[STRIKES] = (strike + 1).coerceAtMost(8)
        npc.timer(TIMER, PlayerVenom.TICK_INTERVAL)
    }
}

package org.rsmod.api.mechanics.toxins.impl

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType

public object NpcVenom {
    private val NoopModifier = NpcHitModifier {}

    public fun isVenomed(npc: Npc): Boolean = npc.vars["varn.venom_strikes"] > 0

    public fun isImmune(npc: Npc): Boolean =
        (npc.visType.paramOrNull(params.venom_immunity) ?: 0) >= constants.npc_venom_full_immunity

    public fun tryVenom(npc: Npc): Boolean {
        if (isImmune(npc) || isVenomed(npc)) {
            return false
        }
        if (NpcPoison.isPoisoned(npc)) {
            NpcPoison.clear(npc)
        }
        npc.vars["varn.venom_strikes"] = 1
        npc.timer("timer.npc_venom", PlayerVenom.TICK_INTERVAL)
        return true
    }

    public fun clear(npc: Npc) {
        npc.vars["varn.venom_strikes"] = 0
        npc.clearTimer("timer.npc_venom")
    }

    public fun onVenomTimerTick(npc: Npc) {
        val strikes = npc.vars["varn.venom_strikes"]
        if (strikes <= 0 || npc.hitpoints <= 0) {
            clear(npc)
            return
        }
        queueVenomHit(npc, PlayerVenom.damageForStrikeIndex(strikes - 1))
        npc.vars["varn.venom_strikes"] = strikes + 1
        npc.timer("timer.npc_venom", PlayerVenom.TICK_INTERVAL)
    }

    private fun queueVenomHit(npc: Npc, damage: Int) {
        npc.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            modifier = NoopModifier,
            hitmark = hitmark_groups.venom,
        )
    }
}

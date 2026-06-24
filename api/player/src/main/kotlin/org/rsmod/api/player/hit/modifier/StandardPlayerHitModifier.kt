package org.rsmod.api.player.hit.modifier

import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

public object StandardPlayerHitModifier : PlayerHitModifier {
    override fun HitBuilder.modify(target: Player) {
        if (target.adminGodMode) {
            damage = 0
            return
        }

        val protectionPrayer =
            when (type) {
                HitType.Typeless -> false
                HitType.Melee -> target.vars["varbit.prayer_protectfrommelee"] == 1
                HitType.Ranged -> target.vars["varbit.prayer_protectfrommissiles"] == 1
                HitType.Magic -> target.vars["varbit.prayer_protectfrommagic"] == 1
            }

        if (protectionPrayer) {
            val reduction = if (isFromPlayer) 40 else 100
            damage = (damage * (100 - reduction)) / 100
        }
    }
}

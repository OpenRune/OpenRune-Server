package org.rsmod.api.player.hit.modifier

import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

public object StandardPlayerHitModifier : PlayerHitModifier {
    override fun HitBuilder.modify(target: Player) {
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

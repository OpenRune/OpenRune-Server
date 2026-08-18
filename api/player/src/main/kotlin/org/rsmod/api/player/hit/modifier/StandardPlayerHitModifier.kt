package org.rsmod.api.player.hit.modifier

import jakarta.inject.Inject
import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hit.PlayerAbsorption
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

public class StandardPlayerHitModifier @Inject constructor(private val eventBus: EventBus) :
    PlayerHitModifier {
    override fun HitBuilder.modify(target: Player) {
        target.publishEvent(this)

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

        if (isFromNpc) {
            damage =
                PlayerAbsorption.absorb(
                    player = target,
                    incomingDamage = damage,
                )
        }
    }

    private fun Player.publishEvent(hit: HitBuilder) {
        val event = PlayerHitEvents.Modify(this, hit)
        eventBus.publish(event)
    }
}

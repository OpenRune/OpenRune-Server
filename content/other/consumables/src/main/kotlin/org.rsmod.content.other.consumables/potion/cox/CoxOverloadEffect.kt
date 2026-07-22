package org.rsmod.content.other.consumables.potion.cox

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.consumables.potion.PotionStatBoost
import org.rsmod.content.other.consumables.potion.overload.OverloadEffectService
import org.rsmod.content.other.consumables.potion.overload.OverloadRegistry
import org.rsmod.content.other.consumables.potion.overload.OverloadType
import org.rsmod.game.entity.Player

@Singleton
class CoxOverloadEffect
@Inject
constructor(
    private val overloads: OverloadEffectService,
) {
    internal fun canApply(
        access: ProtectedAccess,
    ): Boolean =
        overloads.canApply(
            access = access,
            type = OverloadType.COX,
        )

    internal fun apply(
        access: ProtectedAccess,
        tier: CoxPotionTier,
    ) {
        overloads.apply(
            access = access,
            type = OverloadType.COX,
            boost =
                PotionStatBoost(
                    constant = tier.combatConstant,
                    percent = tier.combatPercent,
                ),
            clientTier = tier.clientTier,
        )
    }

    internal fun processDamage(
        access: ProtectedAccess,
    ) {
        overloads.processDamage(
            access = access,
            type = OverloadType.COX,
        )
    }

    internal fun process(
        access: ProtectedAccess,
    ) {
        overloads.process(
            access = access,
            type = OverloadType.COX,
        )
    }

    internal fun clear(
        player: Player,
    ) {
        overloads.clear(
            player = player,
            type = OverloadType.COX,
        )
    }

    companion object {
        val TIMER: String =
            OverloadRegistry.COX_EFFECT_TIMER

        val DAMAGE_TIMER: String =
            OverloadRegistry.COX_DAMAGE_TIMER
    }
}

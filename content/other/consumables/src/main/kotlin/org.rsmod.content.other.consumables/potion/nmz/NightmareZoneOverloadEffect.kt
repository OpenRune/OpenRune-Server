package org.rsmod.content.other.consumables.potion.nmz

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.consumables.potion.overload.OverloadEffectService
import org.rsmod.content.other.consumables.potion.overload.OverloadRegistry
import org.rsmod.content.other.consumables.potion.overload.OverloadType
import org.rsmod.game.entity.Player

@Singleton
class NightmareZoneOverloadEffect
@Inject
constructor(
    private val overloads: OverloadEffectService,
) {
    fun canApply(
        access: ProtectedAccess,
    ): Boolean =
        overloads.canApply(
            access = access,
            type = OverloadType.NIGHTMARE_ZONE,
        )

    fun apply(
        access: ProtectedAccess,
    ) {
        overloads.apply(
            access = access,
            type = OverloadType.NIGHTMARE_ZONE,
        )
    }

    fun processDamage(
        access: ProtectedAccess,
    ) {
        overloads.processDamage(
            access = access,
            type = OverloadType.NIGHTMARE_ZONE,
        )
    }

    fun process(
        access: ProtectedAccess,
    ) {
        overloads.process(
            access = access,
            type = OverloadType.NIGHTMARE_ZONE,
        )
    }

    fun clear(
        player: Player,
    ) {
        overloads.clear(
            player = player,
            type = OverloadType.NIGHTMARE_ZONE,
        )
    }

    companion object {
        val TIMER: String =
            OverloadRegistry.NIGHTMARE_ZONE_EFFECT_TIMER

        val DAMAGE_TIMER: String =
            OverloadRegistry.NIGHTMARE_ZONE_DAMAGE_TIMER
    }
}

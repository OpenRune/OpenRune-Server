package org.rsmod.content.other.consumables.potion

import jakarta.inject.Singleton
import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.player.protect.ProtectedAccess

@Singleton
class RelicymEffect {
    fun handles(handler: String): Boolean =
        handler == HANDLER

    fun apply(
        access: ProtectedAccess,
    ) {
        PlayerDisease.reduceDrain(
            player = access.player,
            amount = DISEASE_REDUCTION_PER_DOSE,
        )
    }

    companion object {
        private const val HANDLER: String =
            "relicyms_balm"

        private const val DISEASE_REDUCTION_PER_DOSE: Int = 1
    }
}

package org.rsmod.content.other.consumables.potion.nmz

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.PotionEffectRow
import org.rsmod.game.entity.Player

@Singleton
class NightmareZonePotionEffect
@Inject
constructor(
    private val overload: NightmareZoneOverloadEffect,
    private val absorption: NightmareZoneAbsorptionEffect,
) {
    fun handles(handler: String): Boolean =
        handler in HANDLERS

    fun canApply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ): Boolean =
        when (effect.handler) {
            HANDLER_OVERLOAD ->
                overload.canApply(access)

            HANDLER_ABSORPTION ->
                absorption.canApply(access)

            else ->
                error(
                    "Unsupported Nightmare Zone potion handler: " +
                        "'${effect.handler}'.",
                )
        }

    fun apply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ) {
        when (effect.handler) {
            HANDLER_OVERLOAD ->
                overload.apply(access)

            HANDLER_ABSORPTION ->
                absorption.apply(access)

            else ->
                error(
                    "Unsupported Nightmare Zone potion handler: " +
                        "'${effect.handler}'.",
                )
        }
    }

    /**
     * Clears the NMZ-only overload state and client counters.
     *
     * The eventual dream session must also clear the underlying
     * PlayerAbsorption points when the dream ends. The currently
     * available API exposes points/add but not a clear operation.
     */
    fun clearSessionEffects(
        player: Player,
    ) {
        overload.clear(player)
        absorption.clearDisplay(player)
    }

    companion object {
        private const val HANDLER_OVERLOAD: String =
            "nzone_overload"

        private const val HANDLER_ABSORPTION: String =
            "nzone_absorption"

        private val HANDLERS: Set<String> =
            setOf(
                HANDLER_OVERLOAD,
                HANDLER_ABSORPTION,
            )
    }
}

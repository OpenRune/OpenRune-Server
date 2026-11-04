package org.rsmod.content.skills.farming

import org.rsmod.api.enums.FarmingEnums
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object FarmingHesporiSignals {
    fun transmit(player: Player, state: PatchState) {
        val indicator = FarmingEnums.farming_hespori_signals.getValue(0)
        val here = player.coords
        if (here.level != indicator.level || here.chebyshevDistance(indicator) > TRANSMIT_RADIUS) return
        VarPlayerIntMapSetter.set(player, TRANSMIT, state.transmit(state.crop, PatchKind.HESPORI))
    }

    private const val TRANSMIT = "varbit.farming_transmit_j"
    private const val TRANSMIT_RADIUS = 52
}

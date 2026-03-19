package org.rsmod.content.areas.city.lumbridge.configs

import dev.openrune.varBit
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player

internal var Player.clueScrollDisableVessels by boolVarBit(LumbridgeVarBits.vesseled_clues_disabled)

internal object LumbridgeVarBits {
    val vesseled_clues_disabled = varBit("vesseled_clues_disabled")
}

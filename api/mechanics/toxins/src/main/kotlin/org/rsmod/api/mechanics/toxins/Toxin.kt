package org.rsmod.api.mechanics.toxins

import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

public object Toxin {

    public fun syncStatusOrbs(player: Player) {
        val poisonVenomOrb = when {
            PlayerVenom.isEnvenomed(player) -> 1_000_000
            PlayerPoison.isPoisoned(player) -> 1
            else -> 0
        }

        VarPlayerIntMapSetter.set(player, "varp.poison", poisonVenomOrb)
        val diseaseOrb = if (PlayerDisease.isDiseased(player)) 1 else 0
        VarPlayerIntMapSetter.set(player, "varp.disease", diseaseOrb)
    }

    public fun Player.cureAllToxins() {
        PlayerPoison.clear(this)
        PlayerVenom.clear(this)
        PlayerDisease.clear(this)
    }
}

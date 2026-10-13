package org.rsmod.content.other.consumables

import jakarta.inject.Singleton
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player

@Singleton
class ConsumableActivityAccess {
    fun canConsume(
        player: Player,
        minigameOnly: String,
        raidOnly: String,
    ): Boolean {
        /**
         *  Activity consumables remain locked behind this class
         *  until the minigame/raid structures are added.
         */
        if (minigameOnly == "gauntlet") return player.insideGauntlet && raidOnly.isBlank()
        if (minigameOnly.isNotBlank()) return false

        if (raidOnly.isNotBlank()) {
            return false
        }

        return true
    }
}

private val Player.insideGauntlet by boolVarBit("varbit.player_in_gauntlet")

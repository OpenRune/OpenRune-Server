package org.rsmod.api.death

import org.rsmod.game.entity.Player

public object PlayerDeathPreviewContext {
    public fun create(
        player: Player,
        protectItem: Boolean,
        skulled: Boolean,
        playerKill: Boolean,
        wildernessLevel: Int,
        inInstance: Boolean,
        inRevenantCaves: Boolean,
        gamemode: Int,
    ): PlayerDeathContext {
        val inWilderness = wildernessLevel > 0
        return PlayerDeathContext(
            player = player,
            coords = player.coords,
            inWilderness = inWilderness,
            wildernessLevel = if (inWilderness) wildernessLevel else -1,
            inRevenantCaves = inRevenantCaves && inWilderness,
            inInstance = inInstance,
            isSkulled = skulled,
            hasProtectItem = protectItem,
            recentPvpDamage = playerKill,
            gamemode = gamemode,
            killer = if (playerKill) player else null,
        )
    }
}

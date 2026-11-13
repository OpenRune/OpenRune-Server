package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player

public fun interface PrayerEnableHook {
    public fun onEnable(player: Player, prayerVarbit: String)
}

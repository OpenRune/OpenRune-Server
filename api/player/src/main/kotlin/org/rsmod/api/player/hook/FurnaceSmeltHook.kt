package org.rsmod.api.player.hook

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

public interface FurnaceSmeltHook {
    public fun claims(player: Player): Boolean

    public suspend fun ProtectedAccess.smelt()
}

package org.rsmod.content.skills.farming

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object FarmingAutoWeed {
    private const val AUTO_WEED = "varbit.farming_blockweeds"
    private const val LOCKED = 0
    private const val DISABLED = 1
    private const val ENABLED = 2

    fun unlocked(player: Player): Boolean = player.vars[AUTO_WEED] != LOCKED

    fun active(player: Player): Boolean = player.vars[AUTO_WEED] == ENABLED

    fun purchaseRefusal(player: Player): String? =
        if (unlocked(player)) "You have already unlocked auto-weed." else null

    fun completePurchase(player: Player) {
        if (!unlocked(player)) VarPlayerIntMapSetter.set(player, AUTO_WEED, ENABLED)
    }

    fun toggle(player: Player) {
        if (!unlocked(player)) return
        VarPlayerIntMapSetter.set(player, AUTO_WEED, if (active(player)) DISABLED else ENABLED)
    }
}

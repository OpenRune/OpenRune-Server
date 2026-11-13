package org.rsmod.content.skills.farming

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object FarmingSeedShopAccess {
    fun recordSuccessfulTheft(player: Player, loc: String) {
        if (loc != "loc.seed_stall") return
        VarPlayerIntMapSetter.set(player, THEFT_UNTIL, Math.toIntExact(epochSeconds() + EMBARGO_SECONDS))
    }

    fun canTrade(player: Player): Boolean = player.vars[THEFT_UNTIL].toLong() <= epochSeconds()

    private fun epochSeconds(): Long = System.currentTimeMillis() / 1000L

    private const val THEFT_UNTIL = "varp.farming_seed_stall_theft_until"
    private const val EMBARGO_SECONDS = 30 * 60L
}

package org.rsmod.content.slayer.rewards

import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/** Slayer reward point balance and client sync. */
object SlayerRewardsPoints {

    fun getPoints(player: Player): Int = player.vars["varbit.slayer_points"]

    fun setPoints(player: Player, amount: Int) {
        VarPlayerIntMapSetter.set(player, "varbit.slayer_points", amount.coerceAtLeast(0))
    }

    fun addPoints(player: Player, amount: Int) {
        if (amount <= 0) return
        setPoints(player, getPoints(player) + amount)
    }

    fun spendPoints(player: Player, amount: Int): Boolean {
        if (amount <= 0) return true
        val current = getPoints(player)
        if (current < amount) return false
        setPoints(player, current - amount)
        return true
    }

    fun syncPoints(player: Player) {
        player.runClientScript(SLAYER_REWARDS_SETPOINTS_CS)
    }

    fun syncPoints(access: ProtectedAccess) {
        syncPoints(access.player)
        update(access.player)
    }

    fun update(player: Player) {
        player.runClientScript(SLAYER_REWARDS_TASKS_INIT_CS)
        VarPlayerIntMapSetter.set(player, "varp.if1", player.vars["varp.slayer_count"])
        VarPlayerIntMapSetter.set(player, "varp.if2", player.vars["varp.slayer_target"])
    }

    private const val SLAYER_REWARDS_SETPOINTS_CS = 409
    private const val SLAYER_REWARDS_TASKS_INIT_CS = 328
}

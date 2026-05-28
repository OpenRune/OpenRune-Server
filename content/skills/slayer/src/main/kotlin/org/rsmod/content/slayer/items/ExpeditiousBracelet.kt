package org.rsmod.content.slayer.items

import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.content.slayer.expeditiousBraceletCharges
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

object ExpeditiousBracelet {

    const val ITEM = "obj.expeditious_bracelet"
    const val MAX_CHARGES = 30
    private const val PROC_ROLL_EXCLUSIVE = 4

    private const val ELITE_COMBAT_ACHIEVEMENTS_COMPLETE = false
    private const val COMBAT_ACHIEVEMENT_RECHARGE_ROLL = 10

    fun isWearing(player: Player): Boolean = player.worn[Wearpos.Hands.slot]?.isType(ITEM) == true

    fun rollExtraKill(player: Player, random: GameRandom): Int {
        if (!isWearing(player)) {
            return 0
        }
        val charges = player.expeditiousBraceletCharges
        if (charges <= 0) {
            return 0
        }
        if (!random.randomBoolean(PROC_ROLL_EXCLUSIVE)) {
            return 0
        }

        val remaining = charges - 1
        player.expeditiousBraceletCharges = remaining.coerceAtLeast(0)

        val suffix =
            when {
                remaining > 0 -> " It has $remaining charges left."
                ELITE_COMBAT_ACHIEVEMENTS_COMPLETE &&
                    random.randomBoolean(COMBAT_ACHIEVEMENT_RECHARGE_ROLL) -> {
                    player.expeditiousBraceletCharges = MAX_CHARGES
                    " It then regenerates itself to full charge!"
                }
                else -> {
                    crumbleWornBracelet(player)
                    " It then crumbles to dust."
                }
            }
        player.mes("Your expeditious bracelet helps you progress your slayer task faster.$suffix",)
        return 1
    }

    fun checkCharges(player: Player) {
        val charges = player.expeditiousBraceletCharges
        if (charges <= 0) {
            player.mes("Your expeditious bracelet has run out of charges.")
            if (isWearing(player)) {
                crumbleWornBracelet(player)
            }
            return
        }
        player.mes("Your expeditious bracelet has $charges charges left.")
    }

    fun prepareBreak(player: Player) {
        player.expeditiousBraceletCharges = MAX_CHARGES
    }

    private fun crumbleWornBracelet(player: Player) {
        val handsSlot = Wearpos.Hands.slot
        if (player.worn[handsSlot]?.isType(ITEM) != true) {
            return
        }
        player.invDel(player.worn, ITEM, count = 1, slot = handsSlot)
    }
}

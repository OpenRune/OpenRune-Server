package org.rsmod.content.slayer.items

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.content.slayer.slaughterBraceletCharges
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

object BraceletOfSlaughter {

    const val ITEM = "obj.bracelet_of_slaughter"
    const val MAX_CHARGES = 30
    private const val PROC_ROLL_EXCLUSIVE = 4

    private val BLOCKED_NPCS = listOf("npc.tzhaar_fightcave_swarm_boss", "npc.inferno_tzkalzuk_placeholder")

    private const val ELITE_COMBAT_ACHIEVEMENTS_COMPLETE = false
    private const val COMBAT_ACHIEVEMENT_RECHARGE_ROLL = 10

    fun isWearing(player: Player): Boolean = player.worn[Wearpos.Hands.slot]?.isType(ITEM) == true

    fun rollSkipTaskDecrement(player: Player, npc: Npc, random: GameRandom): Boolean {
        if (!isWearing(player)) {
            return false
        }
        if (isBlockedNpc(npc)) {
            return false
        }
        val charges = player.slaughterBraceletCharges
        if (charges <= 0) {
            return false
        }
        if (!random.randomBoolean(PROC_ROLL_EXCLUSIVE)) {
            return false
        }

        val remaining = charges - 1
        player.slaughterBraceletCharges = remaining.coerceAtLeast(0)

        val suffix =
            when {
                remaining > 0 -> " It has $remaining charges left."
                ELITE_COMBAT_ACHIEVEMENTS_COMPLETE &&
                    random.randomBoolean(COMBAT_ACHIEVEMENT_RECHARGE_ROLL) -> {
                    player.slaughterBraceletCharges = MAX_CHARGES
                    " It then regenerates itself to full charge!"
                }
                else -> {
                    crumbleWornBracelet(player)
                    " It then crumbles to dust."
                }
            }
        player.mes(
            "Your bracelet of slaughter prevents your slayer count from decreasing.$suffix",
        )
        return true
    }

    fun checkCharges(player: Player) {
        val charges = player.slaughterBraceletCharges
        if (charges <= 0) {
            player.mes("Your bracelet of slaughter has run out of charges.")
            if (isWearing(player)) {
                crumbleWornBracelet(player)
            }
            return
        }
        player.mes("Your bracelet of slaughter has $charges charges left.")
    }

    fun prepareBreak(player: Player) {
        player.slaughterBraceletCharges = MAX_CHARGES
    }

    private fun isBlockedNpc(npc: Npc): Boolean {
        val internal = RSCM.getReverseMapping(RSCMType.NPC, npc.visType.id) ?: return false
        return internal in BLOCKED_NPCS
    }

    private fun crumbleWornBracelet(player: Player) {
        val handsSlot = Wearpos.Hands.slot
        if (player.worn[handsSlot]?.isType(ITEM) != true) {
            return
        }
        player.invDel(player.worn, ITEM, count = 1, slot = handsSlot)
    }
}

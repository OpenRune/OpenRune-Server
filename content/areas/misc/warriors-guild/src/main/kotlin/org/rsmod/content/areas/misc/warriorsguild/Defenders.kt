package org.rsmod.content.areas.misc.warriorsguild

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.enums.WarguildDefenderEnums
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

internal object Defenders {
    const val NONE = -1
    const val RUNE = 6
    const val DRAGON = 7

    private val tiers: Map<Int, Int> =
        WarguildDefenderEnums.warguild_defender_tiers.backing.entries
            .mapNotNull { (obj, tier) -> tier?.let { obj.id to it } }
            .toMap()

    fun drop(tier: Int): String =
        RSCM.getReverseMapping(
            RSCMType.OBJ,
            WarguildDefenderEnums.warguild_defender_drops.getValue(tier).id,
        )

    fun tierOf(obj: Int): Int = tiers[obj] ?: NONE

    fun bestCarried(player: Player): Int = maxOf(best(player.inv), best(player.worn))

    fun bestOwned(player: Player): Int =
        maxOf(bestCarried(player), player.invMap["inv.bank"]?.let(::best) ?: NONE)

    fun remember(player: Player, tier: Int) {
        if (tier + 1 > player.defenderEarned) player.defenderEarned = tier + 1
    }

    private fun best(inv: Inventory): Int = inv.objs.maxOfOrNull { obj -> obj?.let { tierOf(it.id) } ?: NONE } ?: NONE
}

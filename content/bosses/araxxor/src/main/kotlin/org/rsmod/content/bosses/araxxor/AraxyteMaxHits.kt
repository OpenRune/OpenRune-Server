package org.rsmod.content.bosses.araxxor

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

internal class AraxyteMaxHits @Inject constructor(private val bonuses: WornBonuses) {
    fun melee(player: Player, type: MeleeAttackType?): Boolean {
        if (player.righthand.isType("obj.noxious_halberd")) return true
        return type == MeleeAttackType.Crush && highest(player, bonuses.offensiveCrushBonus(player))
    }
    fun ranged(player: Player, type: RangedAttackType?): Boolean =
        type == RangedAttackType.Heavy && highest(player, bonuses.offensiveRangedBonus(player))
    private fun highest(player: Player, chosen: Int): Boolean = chosen >= maxOf(
        bonuses.offensiveStabBonus(player), bonuses.offensiveSlashBonus(player),
        bonuses.offensiveCrushBonus(player), bonuses.offensiveRangedBonus(player),
        bonuses.offensiveMagicBonus(player))
}

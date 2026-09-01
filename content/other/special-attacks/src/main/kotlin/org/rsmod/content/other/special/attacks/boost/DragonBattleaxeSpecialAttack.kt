package org.rsmod.content.other.special.attacks.boost

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository

/**
 * Rampage: trade 10% of the current Attack, Defence, Ranged, and Magic levels for Strength.
 */
class DragonBattleaxeSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant("obj.dragon_battleaxe", ::rampage)
        registerInstant("obj.bh_dragon_battleaxe_corrupted", ::rampage)
    }

    private fun rampage(access: ProtectedAccess): Boolean =
        with(access) {
            val drained = DRAINED_STATS.sumOf { stat ->
                val amount = DragonBattleaxeDrain.drainAmount(stat(stat))
                if (amount > 0) {
                    statSub(stat, constant = amount, percent = 0)
                }
                amount
            }

            statBoost("stat.strength", constant = DragonBattleaxeDrain.strengthBoost(drained), percent = 0)
            anim("seq.rampage")
            spotanim("spotanim.sp_attackglow_red")
            true
        }

    private companion object {
        val DRAINED_STATS =
            listOf(
                "stat.attack",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
            )
    }
}

/**
 * Pure Rampage math, kept separate from [ProtectedAccess] so the drain/boost formula can be unit
 * tested directly instead of only through a live stat roll.
 */
internal object DragonBattleaxeDrain {
    /** Wiki: drains 10% of the current level for each of Attack, Defence, Ranged, and Magic. */
    fun drainAmount(currentLevel: Int): Int = currentLevel / 10

    /** Wiki: Strength is boosted by 10 plus a quarter of the total levels drained. */
    fun strengthBoost(totalDrained: Int): Int = 10 + (totalDrained / 4)
}

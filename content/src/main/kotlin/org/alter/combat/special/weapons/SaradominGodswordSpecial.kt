package org.alter.combat.special.weapons

import org.alter.api.Skills
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class SaradominGodswordSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.sgs")) return@onEvent
            (attacker as Player).animate("sequences.sgs_special_player")
            (attacker as Player).graphic("spotanims.dh_sword_update_saradomin_special_spotanim")
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.sgs")) return@onEvent
            maxHit = (maxHit * 1.1).toInt()
        }
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.sgs")) return@onEvent
            if (!landed || damage <= 0) return@onEvent
            val player = attacker as Player

            // Heal 50% of damage dealt (minimum 10 HP)
            val healAmount = maxOf(10, damage / 2)
            val currentHp = player.getCurrentHp()
            val maxHp = player.getMaxHp()
            player.setCurrentHp(minOf(maxHp, currentHp + healAmount))

            // Restore 25% of damage dealt as prayer points
            val prayerRestore = damage / 4
            val currentPrayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
            val maxPrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
            player.getSkills().setCurrentLevel(Skills.PRAYER, minOf(maxPrayer, currentPrayer + prayerRestore))
        }
    }
}

package org.alter.combat.special.weapons

import org.alter.api.Skills
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class AbyssalBludgeonSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.abyssal_bludgeon")) return@onEvent
            (attacker as Player).animate("sequences.abyssal_bludgeon_special_attack")
            (attacker as Player).graphic("spotanims.abyssal_miasma_spotanim_bludgeon")
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.abyssal_bludgeon")) return@onEvent
            val player = attacker as Player
            val basePrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
            val currentPrayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
            val bonus = (basePrayer - currentPrayer) * 0.5 / 100.0
            maxHit = (maxHit * (1.0 + bonus)).toInt()
        }
    }
}

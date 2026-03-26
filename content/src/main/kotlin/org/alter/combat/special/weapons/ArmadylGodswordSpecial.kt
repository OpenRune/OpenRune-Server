package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class ArmadylGodswordSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.ags", "items.agsg")) return@onEvent
            (attacker as Player).animate("sequences.ags_special_player")
            (attacker as Player).graphic("spotanims.dh_sword_update_armadyl_special_spotanim")
        }
        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, "items.ags", "items.agsg")) return@onEvent
            attackRoll = (attackRoll * 2.0).toInt()
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.ags", "items.agsg")) return@onEvent
            maxHit = (maxHit * 1.375).toInt()
        }
    }
}

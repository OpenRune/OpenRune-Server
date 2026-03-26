package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class DragonWarhammerSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_warhammer")) return@onEvent
            (attacker as Player).animate("sequences.warhammer_special")
            (attacker as Player).graphic("spotanims.sp_attack_warhammer_spotanim", height = 92)
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_warhammer")) return@onEvent
            maxHit = (maxHit * 1.5).toInt()
        }
    }
}

package org.alter.combat.special.weapons

import org.alter.api.HitType
import org.alter.api.ext.hit
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class DragonDaggerSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_dagger")) return@onEvent
            (attacker as Player).animate("sequences.puncture")
            (attacker as Player).graphic("spotanims.sp_attack_puncture_spotanim", height = 92)
        }
        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_dagger")) return@onEvent
            attackRoll = (attackRoll * 1.25).toInt()
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_dagger")) return@onEvent
            maxHit = (maxHit * 1.15).toInt()
        }
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_dagger")) return@onEvent
            // Apply second rapid hit with the same damage as the first
            val hitType = if (landed) HitType.HIT else HitType.BLOCK
            target.hit(damage = damage, type = hitType, delay = 1)
        }
    }
}

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

private val ABYSSAL_DAGGERS = arrayOf(
    "items.abyssal_dagger",
    "items.abyssal_dagger_p",
    "items.abyssal_dagger_p+",
    "items.abyssal_dagger_p++"
)

class AbyssalDaggerSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, *ABYSSAL_DAGGERS)) return@onEvent
            (attacker as Player).animate("sequences.abyssal_dagger_special")
            (attacker as Player).graphic("spotanims.abyssal_dagger_special_spotanim")
        }
        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, *ABYSSAL_DAGGERS)) return@onEvent
            attackRoll = (attackRoll * 1.25).toInt()
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, *ABYSSAL_DAGGERS)) return@onEvent
            maxHit = (maxHit * 0.85).toInt()
        }
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, *ABYSSAL_DAGGERS)) return@onEvent
            // Apply second rapid hit with the same damage as the first
            val hitType = if (landed) HitType.HIT else HitType.BLOCK
            target.hit(damage = damage, type = hitType, delay = 1)
        }
    }
}

package org.alter.combat.special.weapons

import org.alter.api.ext.freeze
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class ZamorakGodswordSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.zgs")) return@onEvent
            (attacker as Player).animate("sequences.zgs_special_player")
            (attacker as Player).graphic("spotanims.dh_sword_update_zamorak_special_spotanim")
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.zgs")) return@onEvent
            maxHit = (maxHit * 1.1).toInt()
        }
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.zgs")) return@onEvent
            if (!landed) return@onEvent
            // Freeze target for 33 ticks (20 seconds)
            target.freeze(cycles = 33)
        }
    }
}

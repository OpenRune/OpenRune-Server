package org.alter.combat.special.weapons

import org.alter.api.Skills
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class BandosGodswordSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.bgs")) return@onEvent
            (attacker as Player).animate("sequences.bgs_special_player")
            (attacker as Player).graphic("spotanims.dh_sword_update_bandos_special_spotanim")
        }
        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.bgs")) return@onEvent
            maxHit = (maxHit * 1.21).toInt()
        }
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.bgs")) return@onEvent
            if (!landed || damage <= 0) return@onEvent

            when (val t = target) {
                is Player -> {
                    // Drain stats in order: Defence, Strength, Prayer, Attack, Magic, Ranged
                    // Each stat drained by up to 20% of its base level
                    var remaining = damage

                    fun drain(skill: Int) {
                        if (remaining <= 0) return
                        val base = t.getSkills().getBaseLevel(skill)
                        val cap = Math.floor(base * 0.20).toInt()
                        val amount = minOf(remaining, cap)
                        if (amount > 0) {
                            val current = t.getSkills().getCurrentLevel(skill)
                            t.getSkills().setCurrentLevel(skill, maxOf(0, current - amount))
                            remaining -= amount
                        }
                    }

                    drain(Skills.DEFENCE)
                    drain(Skills.STRENGTH)
                    drain(Skills.PRAYER)
                    drain(Skills.ATTACK)
                    drain(Skills.MAGIC)
                    drain(Skills.RANGED)
                }
                else -> {
                    // TODO: Implement BGS stat drain for NPCs
                }
            }
        }
    }
}

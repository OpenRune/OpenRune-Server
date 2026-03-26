package org.alter.combat.special.weapons

import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.getVarp
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.message
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent
import kotlin.math.floor

class DragonBattleaxeSpecial : PluginEvent() {

    override fun init() {
        on<PreAttackEvent> {
            where { attacker is Player }
            priority(-15)
            then {
                val player = attacker as Player

                // Only act when special attack bar is active and player has battleaxe equipped
                if (player.getVarp("varp.sa_attack") != 1) return@then
                if (!player.hasEquipped(EquipmentType.WEAPON, "items.dragon_battleaxe")) return@then

                // Check energy (100% required)
                val currentEnergy = player.getVarp("varp.sa_energy") / 10
                if (currentEnergy < 100) {
                    player.message("You don't have enough special attack energy.")
                    player.setVarp("varp.sa_attack", 0)
                    cancelled = true
                    cancelReason = "Insufficient special attack energy"
                    return@then
                }

                // Deduct full 100 energy and disable special bar
                player.setVarp("varp.sa_energy", 0)
                player.setVarp("varp.sa_attack", 0)

                // Drain Attack, Defence, Magic, Ranged by 10% of base level each
                val attackDrain = floor(player.getSkills().getBaseLevel(Skills.ATTACK) * 0.10).toInt()
                val defenceDrain = floor(player.getSkills().getBaseLevel(Skills.DEFENCE) * 0.10).toInt()
                val magicDrain = floor(player.getSkills().getBaseLevel(Skills.MAGIC) * 0.10).toInt()
                val rangedDrain = floor(player.getSkills().getBaseLevel(Skills.RANGED) * 0.10).toInt()

                player.getSkills().setCurrentLevel(
                    Skills.ATTACK,
                    maxOf(0, player.getSkills().getCurrentLevel(Skills.ATTACK) - attackDrain)
                )
                player.getSkills().setCurrentLevel(
                    Skills.DEFENCE,
                    maxOf(0, player.getSkills().getCurrentLevel(Skills.DEFENCE) - defenceDrain)
                )
                player.getSkills().setCurrentLevel(
                    Skills.MAGIC,
                    maxOf(0, player.getSkills().getCurrentLevel(Skills.MAGIC) - magicDrain)
                )
                player.getSkills().setCurrentLevel(
                    Skills.RANGED,
                    maxOf(0, player.getSkills().getCurrentLevel(Skills.RANGED) - rangedDrain)
                )

                // Calculate Strength bonus
                val bonus = 10.0 + floor(0.25 * (attackDrain + defenceDrain + magicDrain + rangedDrain).toDouble())

                // Store bonus for use in combat formula
                player.attr[CombatAttributes.DRAGON_BATTLEAXE_BONUS] = bonus

                // Boost Strength level
                val baseStrength = player.getSkills().getBaseLevel(Skills.STRENGTH)
                val currentStrength = player.getSkills().getCurrentLevel(Skills.STRENGTH)
                val newStrength = minOf(99, maxOf(currentStrength, baseStrength + bonus.toInt()))
                player.getSkills().setCurrentLevel(Skills.STRENGTH, newStrength)

                player.message("You feel a surge of strength!")

                // Cancel the attack — the special IS the action (stat drain only, no hit)
                cancelled = true
                cancelReason = "Dragon battleaxe special activated"
            }
        }
    }
}

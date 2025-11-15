package org.alter.plugins.content.combat.specialattack.weapons.dragonbattleaxe

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.rscm.RSCM.getRSCM

/**
 * Dragon Battleaxe Special Attack
 *
 * Provides a strength bonus based on other combat stats.
 * Formula: 10 + floor(0.25 × (floor(10% magic) + floor(10% range) + floor(10% defence) + floor(10% attack)))
 *
 * After using this special, player cannot use another special attack unless:
 * - Using restoration pool in POH, OR
 * - Another player casts Energy Transfer
 */
class DragonBattleaxePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val SPECIAL_REQUIREMENT = 100 // Dragon battleaxe uses 100% special energy

        SpecialAttacks.register("items.dragon_battleaxe", SPECIAL_REQUIREMENT) {
            player.animate(id = "sequences.battleaxe_crush")
            player.graphic(id = "spotanims.sp_attack_battleaxe_spotanim", height = 92)
            world.spawn(AreaSound(tile = player.tile, id = 2537, radius = 10, volume = 1))

            // Calculate dragon battleaxe strength bonus
            // Formula: 10 + floor(0.25 × (floor(10% magic) + floor(10% range) + floor(10% defence) + floor(10% attack)))
            val magic = player.getSkills().getBaseLevel(Skills.MAGIC)
            val ranged = player.getSkills().getBaseLevel(Skills.RANGED)
            val defence = player.getSkills().getBaseLevel(Skills.DEFENCE)
            val attack = player.getSkills().getBaseLevel(Skills.ATTACK)

            val bonus = 10.0 + Math.floor(0.25 * (
                Math.floor(magic * 0.10) +
                Math.floor(ranged * 0.10) +
                Math.floor(defence * 0.10) +
                Math.floor(attack * 0.10)
            ))

            // Store the bonus in player attributes (will be used in effective strength calculation)
            // This bonus persists until cleared (e.g., by restoration pool or Energy Transfer)
            player.attr[Combat.DRAGON_BATTLEAXE_BONUS] = bonus

            // Apply the strength boost to current level (visual boost)
            val currentStrength = player.getSkills().getCurrentLevel(Skills.STRENGTH)
            val baseStrength = player.getSkills().getBaseLevel(Skills.STRENGTH)
            val newStrength = Math.min(99, (baseStrength + bonus.toInt()).coerceAtLeast(currentStrength))
            player.getSkills().setCurrentLevel(Skills.STRENGTH, newStrength)

            player.message("You feel stronger!")

            // Perform the attack
            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = 1.0)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.0)
            val landHit = accuracy >= world.randomDouble()
            player.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = 1)
        }
    }
}


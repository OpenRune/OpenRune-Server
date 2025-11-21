package org.alter.plugins.content.combat.specialattack.weapons.saradomingodsword

import org.alter.api.Skills
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

class SaradominGodswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val SPECIAL_REQUIREMENT = 50

        SpecialAttacks.register("items.sgs", SPECIAL_REQUIREMENT) {
            player.animate(id = "sequences.sgs_special_player")
            player.graphic(id = "spotanims.dh_sword_update_saradomin_special_spotanim")
            world.spawn(AreaSound(tile = player.tile, id = 3869, radius = 10, volume = 1))

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = 1.1)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.0)
            val landHit = accuracy >= world.randomDouble()
            val delay = 1
            val damage = player.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = delay).hit.hitmarks.sumOf { it.damage }

            // Heal 50% of damage dealt (minimum 10 HP) and restore 25% of damage as prayer
            if (damage > 0) {
                val healAmount = Math.max(10, damage / 2)
                val currentHp = player.getCurrentHp()
                val maxHp = player.getMaxHp()
                player.setCurrentHp(Math.min(maxHp, currentHp + healAmount))

                // Restore prayer points (25% of damage dealt)
                val prayerRestore = damage / 4 // 25% of damage
                val currentPrayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
                val maxPrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
                player.getSkills().setCurrentLevel(Skills.PRAYER, Math.min(maxPrayer, currentPrayer + prayerRestore))
            }
        }
    }
}


package org.alter.plugins.content.combat.specialattack.weapons.bandosgodsword

import org.alter.api.NpcSkills
import org.alter.api.Skills
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

class BandosGodswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val SPECIAL_REQUIREMENT = 50

        SpecialAttacks.register("items.bgs", SPECIAL_REQUIREMENT) {
            player.animate(id = "sequences.bgs_special_player")
            player.graphic(id = "spotanims.dh_sword_update_bandos_special_spotanim")
            world.spawn(AreaSound(tile = player.tile, id = 3869, radius = 10, volume = 1))

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = 1.21)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.0)
            val landHit = accuracy >= world.randomDouble()
            val delay = 1
            val pawnHit = player.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = delay)
            val damage = pawnHit.hit.hitmarks.sumOf { it.damage }

            // Reduce target's stats by damage dealt (up to 20% reduction per stat)
            // Drains in order: Defence, Strength, Prayer, Attack, Magic, Ranged
            if (damage > 0 && pawnHit.landed) {
                var remainingDrain = damage

                when (val targetPawn = target) {
                    is Player -> {
                        val targetPlayer = targetPawn
                        // Drain Defence (up to 20% of base level)
                        val baseDefence = targetPlayer.getSkills().getBaseLevel(Skills.DEFENCE)
                        val maxDefenceDrain = Math.floor(baseDefence * 0.20).toInt()
                        val defenceDrain = Math.min(remainingDrain, maxDefenceDrain)
                        if (defenceDrain > 0) {
                            val currentDefence = targetPlayer.getSkills().getCurrentLevel(Skills.DEFENCE)
                            targetPlayer.getSkills().setCurrentLevel(Skills.DEFENCE, Math.max(0, currentDefence - defenceDrain))
                            remainingDrain -= defenceDrain
                        }

                        // Drain Strength (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseStrength = targetPlayer.getSkills().getBaseLevel(Skills.STRENGTH)
                            val maxStrengthDrain = Math.floor(baseStrength * 0.20).toInt()
                            val strengthDrain = Math.min(remainingDrain, maxStrengthDrain)
                            if (strengthDrain > 0) {
                                val currentStrength = targetPlayer.getSkills().getCurrentLevel(Skills.STRENGTH)
                                targetPlayer.getSkills().setCurrentLevel(Skills.STRENGTH, Math.max(0, currentStrength - strengthDrain))
                                remainingDrain -= strengthDrain
                            }
                        }

                        // Drain Prayer (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val basePrayer = targetPlayer.getSkills().getBaseLevel(Skills.PRAYER)
                            val maxPrayerDrain = Math.floor(basePrayer * 0.20).toInt()
                            val prayerDrain = Math.min(remainingDrain, maxPrayerDrain)
                            if (prayerDrain > 0) {
                                val currentPrayer = targetPlayer.getSkills().getCurrentLevel(Skills.PRAYER)
                                targetPlayer.getSkills().setCurrentLevel(Skills.PRAYER, Math.max(0, currentPrayer - prayerDrain))
                                remainingDrain -= prayerDrain
                            }
                        }

                        // Drain Attack (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseAttack = targetPlayer.getSkills().getBaseLevel(Skills.ATTACK)
                            val maxAttackDrain = Math.floor(baseAttack * 0.20).toInt()
                            val attackDrain = Math.min(remainingDrain, maxAttackDrain)
                            if (attackDrain > 0) {
                                val currentAttack = targetPlayer.getSkills().getCurrentLevel(Skills.ATTACK)
                                targetPlayer.getSkills().setCurrentLevel(Skills.ATTACK, Math.max(0, currentAttack - attackDrain))
                                remainingDrain -= attackDrain
                            }
                        }

                        // Drain Magic (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseMagic = targetPlayer.getSkills().getBaseLevel(Skills.MAGIC)
                            val maxMagicDrain = Math.floor(baseMagic * 0.20).toInt()
                            val magicDrain = Math.min(remainingDrain, maxMagicDrain)
                            if (magicDrain > 0) {
                                val currentMagic = targetPlayer.getSkills().getCurrentLevel(Skills.MAGIC)
                                targetPlayer.getSkills().setCurrentLevel(Skills.MAGIC, Math.max(0, currentMagic - magicDrain))
                                remainingDrain -= magicDrain
                            }
                        }

                        // Drain Ranged (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseRanged = targetPlayer.getSkills().getBaseLevel(Skills.RANGED)
                            val maxRangedDrain = Math.floor(baseRanged * 0.20).toInt()
                            val rangedDrain = Math.min(remainingDrain, maxRangedDrain)
                            if (rangedDrain > 0) {
                                val currentRanged = targetPlayer.getSkills().getCurrentLevel(Skills.RANGED)
                                targetPlayer.getSkills().setCurrentLevel(Skills.RANGED, Math.max(0, currentRanged - rangedDrain))
                            }
                        }
                    }
                    is Npc -> {
                        val targetNpc = targetPawn
                        // Drain Defence (up to 20% of base level)
                        val baseDefence = targetNpc.combatDef.defence
                        val maxDefenceDrain = Math.floor(baseDefence * 0.20).toInt()
                        val defenceDrain = Math.min(remainingDrain, maxDefenceDrain)
                        if (defenceDrain > 0) {
                            val currentDefence = targetNpc.stats.getCurrentLevel(NpcSkills.DEFENCE)
                            targetNpc.stats.setCurrentLevel(NpcSkills.DEFENCE, Math.max(0, currentDefence - defenceDrain))
                            remainingDrain -= defenceDrain
                        }

                        // Drain Strength (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseStrength = targetNpc.combatDef.strength
                            val maxStrengthDrain = Math.floor(baseStrength * 0.20).toInt()
                            val strengthDrain = Math.min(remainingDrain, maxStrengthDrain)
                            if (strengthDrain > 0) {
                                val currentStrength = targetNpc.stats.getCurrentLevel(NpcSkills.STRENGTH)
                                targetNpc.stats.setCurrentLevel(NpcSkills.STRENGTH, Math.max(0, currentStrength - strengthDrain))
                                remainingDrain -= strengthDrain
                            }
                        }

                        // Drain Attack (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseAttack = targetNpc.combatDef.attack
                            val maxAttackDrain = Math.floor(baseAttack * 0.20).toInt()
                            val attackDrain = Math.min(remainingDrain, maxAttackDrain)
                            if (attackDrain > 0) {
                                val currentAttack = targetNpc.stats.getCurrentLevel(NpcSkills.ATTACK)
                                targetNpc.stats.setCurrentLevel(NpcSkills.ATTACK, Math.max(0, currentAttack - attackDrain))
                                remainingDrain -= attackDrain
                            }
                        }

                        // Drain Magic (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseMagic = targetNpc.combatDef.magic
                            val maxMagicDrain = Math.floor(baseMagic * 0.20).toInt()
                            val magicDrain = Math.min(remainingDrain, maxMagicDrain)
                            if (magicDrain > 0) {
                                val currentMagic = targetNpc.stats.getCurrentLevel(NpcSkills.MAGIC)
                                targetNpc.stats.setCurrentLevel(NpcSkills.MAGIC, Math.max(0, currentMagic - magicDrain))
                                remainingDrain -= magicDrain
                            }
                        }

                        // Drain Ranged (up to 20% of base level)
                        if (remainingDrain > 0) {
                            val baseRanged = targetNpc.combatDef.ranged
                            val maxRangedDrain = Math.floor(baseRanged * 0.20).toInt()
                            val rangedDrain = Math.min(remainingDrain, maxRangedDrain)
                            if (rangedDrain > 0) {
                                val currentRanged = targetNpc.stats.getCurrentLevel(NpcSkills.RANGED)
                                targetNpc.stats.setCurrentLevel(NpcSkills.RANGED, Math.max(0, currentRanged - rangedDrain))
                            }
                        }
                    }
                }
            }
        }
    }
}


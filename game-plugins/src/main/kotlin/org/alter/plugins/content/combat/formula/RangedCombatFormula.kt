package org.alter.plugins.content.combat.formula

import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.mechanics.prayer.Prayer
import org.alter.plugins.content.mechanics.prayer.Prayers

/**
 * @author Tom <rspsmods@gmail.com>
 */
object RangedCombatFormula : CombatFormula {
    private val BLACK_MASKS =
        arrayOf(
            "items.harmless_black_mask",
            "items.harmless_black_mask_1",
            "items.harmless_black_mask_2",
            "items.harmless_black_mask_3",
            "items.harmless_black_mask_4",
            "items.harmless_black_mask_5",
            "items.harmless_black_mask_6",
            "items.harmless_black_mask_7",
            "items.harmless_black_mask_8",
            "items.harmless_black_mask_9",
            "items.harmless_black_mask_10",
        )

    private val BLACK_MASKS_I =
        arrayOf(
            "items.nzone_black_mask",
            "items.nzone_black_mask_1",
            "items.nzone_black_mask_2",
            "items.nzone_black_mask_3",
            "items.nzone_black_mask_4",
            "items.nzone_black_mask_5",
            "items.nzone_black_mask_6",
            "items.nzone_black_mask_7",
            "items.nzone_black_mask_8",
            "items.nzone_black_mask_9",
            "items.nzone_black_mask_10",
        )

    private val RANGED_VOID = arrayOf("items.game_pest_archer_helm", "items.pest_void_knight_top", "items.pest_void_knight_robes", "items.pest_void_knight_gloves")

    private val RANGED_ELITE_VOID =
        arrayOf("items.game_pest_archer_helm", "items.elite_void_knight_top", "items.elite_void_knight_robes", "items.pest_void_knight_gloves")

    override fun getAccuracy(
        pawn: Pawn,
        target: Pawn,
        specialAttackMultiplier: Double,
    ): Double {
        val attack = getAttackRoll(pawn, target, specialAttackMultiplier)
        val defence = getDefenceRoll(pawn, target)

        val accuracy: Double
        if (attack > defence) {
            accuracy = 1.0 - (defence + 2.0) / (2.0 * (attack + 1.0))
        } else {
            accuracy = attack / (2.0 * (defence + 1))
        }
        return accuracy
    }

    override fun getMaxHit(
        pawn: Pawn,
        target: Pawn,
        specialAttackMultiplier: Double,
        specialPassiveMultiplier: Double,
    ): Int {
        val a =
            if (pawn is Player) {
                getEffectiveRangedLevel(pawn)
            } else if (pawn is Npc) {
                getEffectiveRangedLevel(pawn)
            } else {
                0.0
            }
        val b = getEquipmentRangedBonus(pawn)

        var base = Math.floor(0.5 + a * (b + 64.0) / 640.0).toInt()
        if (pawn is Player) {
            base = applyRangedSpecials(pawn, target, base, specialAttackMultiplier, specialPassiveMultiplier)
        }
        return base
    }

    private fun getAttackRoll(
        pawn: Pawn,
        target: Pawn,
        specialAttackMultiplier: Double,
    ): Int {
        val a =
            if (pawn is Player) {
                getEffectiveAttackLevel(pawn)
            } else if (pawn is Npc) {
                getEffectiveAttackLevel(pawn)
            } else {
                0.0
            }
        val b = getEquipmentAttackBonus(pawn)

        var maxRoll = a * (b + 64.0)
        if (pawn is Player) {
            maxRoll = applyAttackSpecials(pawn, target, maxRoll, specialAttackMultiplier)
        }
        return maxRoll.toInt()
    }

    private fun getDefenceRoll(
        pawn: Pawn,
        target: Pawn,
    ): Int {
        val a =
            if (pawn is Player) {
                getEffectiveDefenceLevel(pawn)
            } else if (pawn is Npc) {
                getEffectiveDefenceLevel(pawn)
            } else {
                0.0
            }
        val b = getEquipmentDefenceBonus(target)

        var maxRoll = a * (b + 64.0)
        maxRoll = applyDefenceSpecials(target, maxRoll)
        return maxRoll.toInt()
    }

    private fun applyRangedSpecials(
        player: Player,
        target: Pawn,
        base: Int,
        specialAttackMultiplier: Double,
        specialPassiveMultiplier: Double,
    ): Int {
        var hit = base.toDouble()

        hit *= getEquipmentMultiplier(player)
        hit = Math.floor(hit)

        if (specialAttackMultiplier == 1.0) {
            val multiplier =
                when {
                    player.hasEquipped(EquipmentType.WEAPON, "items.dragonhunter_xbow") && isDragon(target) -> 1.3
                    player.hasEquipped(EquipmentType.WEAPON, "items.twisted_bow") && target.entityType.isNpc -> {
                        // TODO: cap inside Chambers of Xeric is 350
                        val cap = 250.0
                        val magic =
                            when (target) {
                                is Player -> target.getSkills().getCurrentLevel(Skills.MAGIC)
                                is Npc -> target.combatDef.magic
                                else -> throw IllegalStateException("Invalid pawn type. [$target]")
                            }
                        val modifier =
                            Math.min(
                                cap,
                                250.0 + (((magic * 3.0) - 14.0) / 100.0) - (Math.pow((((magic * 3.0) / 10.0) - 140.0), 2.0) / 100.0),
                            )
                        modifier
                    }
                    else -> 1.0
                }
            hit *= multiplier
            hit = Math.floor(hit)
        } else {
            hit *= specialAttackMultiplier
            hit = Math.floor(hit)
        }

        // Protection prayers: 100% reduction in PvM, 40% reduction (0.6 multiplier) in PvP
        if (target.hasPrayerIcon(PrayerIcon.PROTECT_FROM_MISSILES)) {
            if (target.entityType.isNpc) {
                // PvM: 100% damage reduction (0 damage)
                hit = 0.0
            } else {
                // PvP: 40% damage reduction (multiply by 0.6)
                hit *= 0.6
                hit = Math.floor(hit)
            }
        }

        if (specialPassiveMultiplier == 1.0) {
            hit = applyPassiveMultiplier(player, target, hit)
            hit = Math.floor(hit)
        } else {
            hit *= specialPassiveMultiplier
            hit = Math.floor(hit)
        }

        hit *= getDamageDealMultiplier(player)
        hit = Math.floor(hit)

        hit *= getDamageTakeMultiplier(target)
        hit = Math.floor(hit)

        return hit.toInt()
    }

    private fun applyAttackSpecials(
        player: Player,
        target: Pawn,
        base: Double,
        specialAttackMultiplier: Double,
    ): Double {
        var hit = base

        hit *= getEquipmentMultiplier(player)
        hit = Math.floor(hit)

        if (specialAttackMultiplier == 1.0) {
            val multiplier =
                when {
                    player.hasEquipped(EquipmentType.WEAPON, "items.dragonhunter_xbow") && isDragon(target) -> 1.3
                    player.hasEquipped(EquipmentType.WEAPON, "items.twisted_bow") && target.entityType.isNpc -> {
                        // TODO: cap inside Chambers of Xeric is 250
                        val cap = 140.0
                        val magic =
                            when (target) {
                                is Player -> target.getSkills().getCurrentLevel(Skills.MAGIC)
                                is Npc -> target.combatDef.magic
                                else -> throw IllegalStateException("Invalid pawn type. [$target]")
                            }
                        val modifier =
                            Math.min(
                                cap,
                                140.0 + (((magic * 3.0) - 10.0) / 100.0) - (Math.pow((((magic * 3.0) / 10.0) - 100.0), 2.0) / 100.0),
                            )
                        modifier
                    }
                    else -> 1.0
                }
            hit *= multiplier
            hit = Math.floor(hit)
        } else {
            hit *= specialAttackMultiplier
            hit = Math.floor(hit)
        }

        return hit
    }

    private fun applyDefenceSpecials(
        target: Pawn,
        base: Double,
    ): Double {
        var hit = base

        if (target is Player && isWearingTorag(target) && target.hasEquipped(EquipmentType.AMULET, "items.damned_amulet")) {
            val lost = (target.getMaxHp() - target.getCurrentHp()) / 100.0
            val max = target.getMaxHp() / 100.0
            hit *= (1.0 + (lost * max))
            hit = Math.floor(hit)
        }

        return hit
    }

    private fun getEquipmentRangedBonus(pawn: Pawn): Double =
        when (pawn) {
            is Player -> pawn.getRangedStrengthBonus().toDouble()
            is Npc -> pawn.getRangedStrengthBonus().toDouble()
            else -> throw IllegalArgumentException("Invalid pawn type. $pawn")
        }

    private fun getEquipmentAttackBonus(pawn: Pawn): Double {
        return pawn.getBonus(BonusSlot.ATTACK_RANGED).toDouble()
    }

    private fun getEquipmentDefenceBonus(target: Pawn): Double {
        return target.getBonus(BonusSlot.DEFENCE_RANGED).toDouble()
    }

    private fun getEffectiveRangedLevel(player: Player): Double {
        // OSRS Formula: floor((floor(floor(Ranged Level + Potion Bonus) × Prayer Bonus) + Style Bonus + 8) × Void Bonus)
        val baseLevel = player.getSkills().getBaseLevel(Skills.RANGED).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.RANGED).toDouble()
        // Calculate potion bonus (difference between current and base level)
        val potionBonus = Math.max(0.0, currentLevel - baseLevel)

        // Add potion bonus to base level
        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        // Apply prayer multiplier
        effectiveLevel = Math.floor(effectiveLevel * getPrayerRangedMultiplier(player))

        // Add style bonus
        effectiveLevel +=
            when (CombatConfigs.getAttackStyle(player)) {
                AttackStyle.ACCURATE -> 3.0
                else -> 0.0
            }

        // Add 8
        effectiveLevel += 8.0

        // Apply void bonus
        if (player.hasEquipped(RANGED_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.10)
        } else if (player.hasEquipped(RANGED_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.125)
        }

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveAttackLevel(player: Player): Double {
        // OSRS Formula: floor((floor(floor(Ranged Level + Potion Bonus) × Prayer Bonus) + Style Bonus + 8) × Void Bonus)
        val baseLevel = player.getSkills().getBaseLevel(Skills.RANGED).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.RANGED).toDouble()
        // Calculate potion bonus (difference between current and base level)
        val potionBonus = Math.max(0.0, currentLevel - baseLevel)

        // Add potion bonus to base level
        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        // Apply prayer multiplier
        effectiveLevel = Math.floor(effectiveLevel * getPrayerAttackMultiplier(player))

        // Add style bonus
        effectiveLevel +=
            when (CombatConfigs.getAttackStyle(player)) {
                AttackStyle.ACCURATE -> 3.0
                else -> 0.0
            }

        // Add 8
        effectiveLevel += 8.0

        // Apply void bonus
        if (player.hasEquipped(RANGED_VOID) || player.hasEquipped(RANGED_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.10)
        }

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveDefenceLevel(player: Player): Double {
        // OSRS Formula: floor((floor(floor(Defence Level + Potion Bonus) × Prayer Bonus) + Style Bonus + 8))
        val baseLevel = player.getSkills().getBaseLevel(Skills.DEFENCE).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.DEFENCE).toDouble()
        // Calculate potion bonus (difference between current and base level)
        val potionBonus = Math.max(0.0, currentLevel - baseLevel)

        // Add potion bonus to base level
        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        // Apply prayer multiplier
        effectiveLevel = Math.floor(effectiveLevel * getPrayerDefenceMultiplier(player))

        // Add style bonus
        effectiveLevel +=
            when (CombatConfigs.getAttackStyle(player)) {
                AttackStyle.DEFENSIVE -> 3.0
                AttackStyle.CONTROLLED -> 1.0
                AttackStyle.LONG_RANGE -> 3.0
                else -> 0.0
            }

        // Add 8
        effectiveLevel += 8.0

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveRangedLevel(npc: Npc): Double {
        // NPC effective ranged = base ranged + 8
        var effectiveLevel = npc.combatDef.ranged.toDouble()
        effectiveLevel += 8
        return effectiveLevel
    }

    private fun getEffectiveAttackLevel(npc: Npc): Double {
        // NPC effective attack = base ranged + 8 (for ranged attacks)
        var effectiveLevel = npc.combatDef.ranged.toDouble()
        effectiveLevel += 8
        return effectiveLevel
    }

    private fun getEffectiveDefenceLevel(npc: Npc): Double {
        // NPC effective defence = base defence + 8
        var effectiveLevel = npc.combatDef.defence.toDouble()
        effectiveLevel += 8
        return effectiveLevel
    }

    private fun getPrayerRangedMultiplier(player: Player): Double =
        when {
            Prayers.isActive(player, Prayer.SHARP_EYE) -> 1.05
            Prayers.isActive(player, Prayer.HAWK_EYE) -> 1.10
            Prayers.isActive(player, Prayer.EAGLE_EYE) -> 1.15
            Prayers.isActive(player, Prayer.RIGOUR) -> 1.23
            else -> 1.0
        }

    private fun getPrayerAttackMultiplier(player: Player): Double =
        when {
            Prayers.isActive(player, Prayer.SHARP_EYE) -> 1.05
            Prayers.isActive(player, Prayer.HAWK_EYE) -> 1.10
            Prayers.isActive(player, Prayer.EAGLE_EYE) -> 1.15
            Prayers.isActive(player, Prayer.RIGOUR) -> 1.20
            else -> 1.0
        }

    private fun getPrayerDefenceMultiplier(player: Player): Double =
        when {
            Prayers.isActive(player, Prayer.THICK_SKIN) -> 1.05
            Prayers.isActive(player, Prayer.ROCK_SKIN) -> 1.10
            Prayers.isActive(player, Prayer.STEEL_SKIN) -> 1.15
            Prayers.isActive(player, Prayer.CHIVALRY) -> 1.20
            Prayers.isActive(player, Prayer.PIETY) -> 1.25
            Prayers.isActive(player, Prayer.RIGOUR) -> 1.25
            Prayers.isActive(player, Prayer.AUGURY) -> 1.25
            else -> 1.0
        }

    private fun getEquipmentMultiplier(player: Player): Double =
        when {
            player.hasEquipped(EquipmentType.AMULET, "items.crystalshard_necklace") -> 7.0 / 6.0
            player.hasEquipped(EquipmentType.AMULET, "items.lotr_crystalshard_necklace_upgrade") -> 1.2
            player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet") -> 1.15
            player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet_e") -> 1.2
            // TODO: this should only apply when target is slayer task?
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS) -> 7.0 / 6.0
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS_I) -> 1.15
            else -> 1.0
        }

    private fun applyPassiveMultiplier(
        player: Player,
        target: Pawn,
        base: Double,
    ): Double {
        when {
            player.hasWeaponType(WeaponType.CROSSBOW) && player.attr.has(Combat.BOLT_ENCHANTMENT_EFFECT) -> {
                val dragonstone =
                    player.hasEquipped(
                        EquipmentType.AMMO,
                        "items.xbows_crossbow_bolts_runite_tipped_dragonstone",
                        "items.xbows_crossbow_bolts_runite_tipped_dragonstone_enchanted",
                        "items.dragon_bolts_unenchanted_dragonstone",
                        "items.dragon_bolts_enchanted_dragonstone",
                    )
                val opal =
                    player.hasEquipped(
                        EquipmentType.AMMO,
                        "items.opal_bolt",
                        "items.xbows_crossbow_bolts_bronze_tipped_opal_enchanted",
                        "items.dragon_bolts_unenchanted_opal",
                        "items.dragon_bolts_enchanted_opal",
                    )
                val pearl =
                    player.hasEquipped(
                        EquipmentType.AMMO,
                        "items.pearl_bolt",
                        "items.xbows_crossbow_bolts_iron_tipped_pearl_enchanted",
                        "items.dragon_bolts_unenchanted_pearl",
                        "items.dragon_bolts_enchanted_pearl",
                    )

                when {
                    dragonstone -> return base + Math.floor(player.getSkills().getCurrentLevel(Skills.RANGED) / 5.0)
                    opal -> return base + Math.floor(player.getSkills().getCurrentLevel(Skills.RANGED) / 10.0)
                    pearl ->
                        return base +
                            Math.floor(
                                player.getSkills().getCurrentLevel(Skills.RANGED) / (if (isFiery(target)) 15.0 else 20.0),
                            )
                }
            }
        }
        return base
    }

    private fun getDamageDealMultiplier(pawn: Pawn): Double = pawn.attr[Combat.DAMAGE_DEAL_MULTIPLIER] ?: 1.0

    private fun getDamageTakeMultiplier(pawn: Pawn): Double = pawn.attr[Combat.DAMAGE_TAKE_MULTIPLIER] ?: 1.0

    private fun isDragon(pawn: Pawn): Boolean {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).isSpecies(NpcSpecies.DRACONIC)
        }
        return false
    }

    private fun isFiery(pawn: Pawn): Boolean {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).isSpecies(NpcSpecies.FIERY)
        }
        return false
    }

    private fun isWearingTorag(player: Player): Boolean {
        return player.hasEquipped(
            EquipmentType.HEAD,
            "items.barrows_torag_head",
            "items.barrows_torag_head_25",
            "items.barrows_torag_head_50",
            "items.barrows_torag_head_75",
            "items.barrows_torag_head_100",
        ) &&
            player.hasEquipped(
                EquipmentType.WEAPON,
                "items.barrows_torag_weapon",
                "items.barrows_torag_weapon_25",
                "items.barrows_torag_weapon_50",
                "items.barrows_torag_weapon_75",
                "items.barrows_torag_weapon_100",
            ) &&
            player.hasEquipped(
                EquipmentType.CHEST,
                "items.barrows_torag_body",
                "items.barrows_torag_body_25",
                "items.barrows_torag_body_50",
                "items.barrows_torag_body_75",
                "items.barrows_torag_body_100",
            ) &&
            player.hasEquipped(
                EquipmentType.LEGS,
                "items.barrows_torag_legs",
                "items.barrows_torag_legs_25",
                "items.barrows_torag_legs_50",
                "items.barrows_torag_legs_75",
                "items.barrows_torag_legs_100",
            )
    }
}

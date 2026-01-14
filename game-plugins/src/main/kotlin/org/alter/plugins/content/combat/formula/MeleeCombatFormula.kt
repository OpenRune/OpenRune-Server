package org.alter.plugins.content.combat.formula

import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.mechanics.prayer.Prayer
import org.alter.plugins.content.mechanics.prayer.Prayers

/**
 * @author Tom <rspsmods@gmail.com>
 */
object MeleeCombatFormula : CombatFormula {

    private val BLACK_MASKS = arrayOf("items.harmless_black_mask",
            "items.harmless_black_mask_1", "items.harmless_black_mask_2", "items.harmless_black_mask_3", "items.harmless_black_mask_4",
            "items.harmless_black_mask_5", "items.harmless_black_mask_6", "items.harmless_black_mask_7", "items.harmless_black_mask_8",
            "items.harmless_black_mask_9", "items.harmless_black_mask_10")

    private val BLACK_MASKS_I = arrayOf("items.nzone_black_mask",
            "items.nzone_black_mask_1", "items.nzone_black_mask_2", "items.nzone_black_mask_3", "items.nzone_black_mask_4",
            "items.nzone_black_mask_5", "items.nzone_black_mask_6", "items.nzone_black_mask_7", "items.nzone_black_mask_8",
            "items.nzone_black_mask_9", "items.nzone_black_mask_10")

    private val MELEE_VOID = arrayOf("items.game_pest_melee_helm", "items.pest_void_knight_top", "items.pest_void_knight_robes", "items.pest_void_knight_gloves")

    private val MELEE_ELITE_VOID = arrayOf("items.game_pest_melee_helm", "items.elite_void_knight_top", "items.elite_void_knight_robes", "items.pest_void_knight_gloves")

    override fun getAccuracy(pawn: Pawn, target: Pawn, specialAttackMultiplier: Double): Double {
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

    override fun getMaxHit(pawn: Pawn, target: Pawn, specialAttackMultiplier: Double, specialPassiveMultiplier: Double): Int {
        val a = if (pawn is Player) getEffectiveStrengthLevel(pawn) else if (pawn is Npc) getEffectiveStrengthLevel(pawn) else 0.0
        val b = getEquipmentStrengthBonus(pawn)

        var base = Math.floor(0.5 + a * (b + 64.0) / 640.0).toInt()
        if (pawn is Player) {
            base = applyStrengthSpecials(pawn, target, base, specialAttackMultiplier, specialPassiveMultiplier)
        }
        return base
    }

    private fun getAttackRoll(pawn: Pawn, target: Pawn, specialAttackMultiplier: Double): Int {
        val a = if (pawn is Player) getEffectiveAttackLevel(pawn) else if (pawn is Npc) getEffectiveAttackLevel(pawn) else 0.0
        val b = getEquipmentAttackBonus(pawn)

        var maxRoll = a * (b + 64.0)
        if (pawn is Player) {
            maxRoll = applyAttackSpecials(pawn, target, maxRoll, specialAttackMultiplier)
        }
        return maxRoll.toInt()
    }

    private fun getDefenceRoll(pawn: Pawn, target: Pawn): Int {
        val a = if (pawn is Player) getEffectiveDefenceLevel(pawn) else if (pawn is Npc) getEffectiveDefenceLevel(pawn) else 0.0
        val b = getEquipmentDefenceBonus(pawn, target)

        var maxRoll = a * (b + 64.0)
        maxRoll = applyDefenceSpecials(target, maxRoll)
        return maxRoll.toInt()
    }

    private fun applyStrengthSpecials(player: Player, target: Pawn, base: Int, specialAttackMultiplier: Double, specialPassiveMultiplier: Double): Int {
        var hit = base.toDouble()

        hit *= getEquipmentMultiplier(player)
        hit = Math.floor(hit)

        hit *= specialAttackMultiplier
        hit = Math.floor(hit)

        // Protection prayers: 100% reduction in PvM, 40% reduction (0.6 multiplier) in PvP
        if (target.hasPrayerIcon(PrayerIcon.PROTECT_FROM_MELEE)) {
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

    private fun applyAttackSpecials(player: Player, target: Pawn, base: Double, specialAttackMultiplier: Double): Double {
        var hit = base

        hit *= getEquipmentMultiplier(player)
        hit = Math.floor(hit)

        // Apply slayer helmet/black mask bonus for accuracy
        hit *= getSlayerHelmetMultiplier(player, target)
        hit = Math.floor(hit)

        hit *= (if (player.hasEquipped(Wearpos.RightHand, "items.arclight") && isDemon(target)) 1.7 else specialAttackMultiplier)
        hit = Math.floor(hit)

        return hit
    }

    private fun applyDefenceSpecials(target: Pawn, base: Double): Double {
        var hit = base

        if (target is Player && isWearingTorag(target) && target.hasEquipped(Wearpos.Front, "items.damned_amulet")) {
            val lost = (target.getMaxHp() - target.getCurrentHp()) / 100.0
            val max = target.getMaxHp() / 100.0
            hit *= (1.0 + (lost * max))
            hit = Math.floor(hit)
        }

        return hit
    }

    private fun getEquipmentStrengthBonus(pawn: Pawn): Double = when (pawn) {
        is Player -> pawn.getStrengthBonus().toDouble()
        is Npc -> pawn.getStrengthBonus().toDouble()
        else -> throw IllegalArgumentException("Invalid pawn type. $pawn")
    }

    private fun getEquipmentAttackBonus(pawn: Pawn): Double {
        val combatStyle = CombatConfigs.getCombatStyle(pawn)
        val bonus = 0
        return 0.0
    }

    private fun getEquipmentDefenceBonus(pawn: Pawn, target: Pawn): Double {
        val combatStyle = CombatConfigs.getCombatStyle(pawn)

        return 0.0
    }

    private fun getEffectiveStrengthLevel(player: Player): Double {
        // OSRS Formula: floor((floor(floor(Strength Level + Potion Bonus + Dragon Battleaxe Bonus) × Prayer Bonus) + Style Bonus + 8) × Void Bonus)
        val baseLevel = player.getSkills().getBaseLevel(Skills.STRENGTH).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.STRENGTH).toDouble()
        // Calculate potion bonus (difference between current and base level)
        var potionBonus = Math.max(0.0, currentLevel - baseLevel)

        // Add dragon battleaxe special attack bonus if active
        // Formula: 10 + floor(0.25 × (floor(10% magic) + floor(10% range) + floor(10% defence) + floor(10% attack)))
        val dragonBattleaxeBonus = player.attr[Combat.DRAGON_BATTLEAXE_BONUS] ?: 0.0
        if (dragonBattleaxeBonus > 0.0) {
            potionBonus += dragonBattleaxeBonus
        }

        // Add potion bonus (and dragon battleaxe bonus) to base level
        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        // Apply prayer multiplier
        effectiveLevel = Math.floor(effectiveLevel * getPrayerStrengthMultiplier(player))


        // Add 8
        effectiveLevel += 8.0

        // Apply void bonus
        if (player.hasEquipped(MELEE_VOID) || player.hasEquipped(MELEE_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.10)
        }

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveAttackLevel(player: Player): Double {
        // OSRS Formula: floor((floor(floor(Attack Level + Potion Bonus) × Prayer Bonus) + Style Bonus + 8) × Void Bonus)
        val baseLevel = player.getSkills().getBaseLevel(Skills.ATTACK).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.ATTACK).toDouble()
        // Calculate potion bonus (difference between current and base level)
        val potionBonus = Math.max(0.0, currentLevel - baseLevel)

        // Add potion bonus to base level
        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        // Apply prayer multiplier
        effectiveLevel = Math.floor(effectiveLevel * getPrayerAttackMultiplier(player))


        // Add 8
        effectiveLevel += 8.0

        // Apply void bonus
        if (player.hasEquipped(MELEE_VOID) || player.hasEquipped(MELEE_ELITE_VOID)) {
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


        // Add 8
        effectiveLevel += 8.0

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveStrengthLevel(npc: Npc): Double {
        // NPC effective strength = base strength + 8
        var effectiveLevel = npc.combatDef.strength.toDouble()
        effectiveLevel += 8
        return effectiveLevel
    }

    private fun getEffectiveAttackLevel(npc: Npc): Double {
        // NPC effective attack = base attack + 8
        var effectiveLevel = npc.combatDef.attack.toDouble()
        effectiveLevel += 8
        return effectiveLevel
    }

    private fun getEffectiveDefenceLevel(npc: Npc): Double {
        // NPC effective defence = base defence + 8
        var effectiveLevel = npc.combatDef.defence.toDouble()
        effectiveLevel += 8
        return effectiveLevel
    }

    private fun getPrayerStrengthMultiplier(player: Player): Double = when {
        Prayers.isActive(player, Prayer.BURST_OF_STRENGTH) -> 1.05
        Prayers.isActive(player, Prayer.SUPERHUMAN_STRENGTH) -> 1.10
        Prayers.isActive(player, Prayer.ULTIMATE_STRENGTH) -> 1.15
        Prayers.isActive(player, Prayer.CHIVALRY) -> 1.18
        Prayers.isActive(player, Prayer.PIETY) -> 1.23
        else -> 1.0
    }

    private fun getPrayerAttackMultiplier(player: Player): Double = when {
        Prayers.isActive(player, Prayer.CLARITY_OF_THOUGHT) -> 1.05
        Prayers.isActive(player, Prayer.IMPROVED_REFLEXES) -> 1.10
        Prayers.isActive(player, Prayer.INCREDIBLE_REFLEXES) -> 1.15
        Prayers.isActive(player, Prayer.CHIVALRY) -> 1.15
        Prayers.isActive(player, Prayer.PIETY) -> 1.20
        else -> 1.0
    }

    private fun getPrayerDefenceMultiplier(player: Player): Double = when {
        Prayers.isActive(player, Prayer.THICK_SKIN) -> 1.05
        Prayers.isActive(player, Prayer.ROCK_SKIN) -> 1.10
        Prayers.isActive(player, Prayer.STEEL_SKIN) -> 1.15
        Prayers.isActive(player, Prayer.CHIVALRY) -> 1.20
        Prayers.isActive(player, Prayer.PIETY) -> 1.25
        Prayers.isActive(player, Prayer.RIGOUR) -> 1.25
        Prayers.isActive(player, Prayer.AUGURY) -> 1.25
        else -> 1.0
    }

    private fun getEquipmentMultiplier(player: Player): Double = when {
        player.hasEquipped(Wearpos.Front, "items.crystalshard_necklace") -> 7.0 / 6.0 // These should only apply if the target is undead..
        player.hasEquipped(Wearpos.Front, "items.lotr_crystalshard_necklace_upgrade") -> 1.2 // These should only apply if the target is undead..
        else -> 1.0
    }

    /**
     * Check if black mask/slayer helmet bonus should apply.
     * Bonus only applies when player is on a slayer task for the target NPC.
     */
    private fun hasSlayerTaskBonus(player: Player, target: Pawn): Boolean {
        // TODO: Implement slayer task system check
        // Should check if:
        // 1. Player has an active slayer task
        // 2. Target NPC matches the slayer task (exact match or category match)
        // 3. Task is not completed
        // For now, return false until slayer task system is implemented
        return false
    }

    /**
     * Get black mask/slayer helmet multiplier if applicable.
     * Black mask: 16.67% (7/6) boost
     * Slayer helmet (imbued): 16.67% (7/6) boost
     * Slayer helmet (regular): 15% boost
     */
    private fun getSlayerHelmetMultiplier(player: Player, target: Pawn): Double {
        if (!hasSlayerTaskBonus(player, target)) {
            return 1.0
        }
        return when {
            player.hasEquipped(Wearpos.Hat, *BLACK_MASKS) || player.hasEquipped(Wearpos.Hat, *BLACK_MASKS_I) -> 7.0 / 6.0
            // TODO: Add slayer helmet checks when items are identified
            else -> 1.0
        }
    }

    private fun applyPassiveMultiplier(pawn: Pawn, target: Pawn, base: Double): Double {
        if (pawn is Player) {
            val world = pawn.world
            var multiplier = when {
                pawn.hasEquipped(Wearpos.Front, "items.jewl_beserker_necklace") -> 1.2
                isWearingDharok(pawn) -> {
                    val lost = (pawn.getMaxHp() - pawn.getCurrentHp()) / 100.0
                    val max = pawn.getMaxHp() / 100.0
                    1.0 + (lost * max)
                }
                pawn.hasEquipped(Wearpos.RightHand, "items.gadderanks_warhammer") && isShade(target) -> if (world.chance(1, 20)) 2.0 else 1.25
                pawn.hasEquipped(Wearpos.RightHand, "items.contact_keris", "items.contact_keris_p") && (isKalphite(target) || isScarab(target)) -> if (world.chance(1, 51)) 3.0 else (4.0 / 3.0)
                else -> 1.0
            }

            // Apply slayer helmet/black mask bonus
            multiplier *= getSlayerHelmetMultiplier(pawn, target)

            if (multiplier == 1.0 && isWearingVerac(pawn)) {
                return base + 1.0
            }
            return base * multiplier
        }
        return base
    }

    private fun getDamageDealMultiplier(pawn: Pawn): Double = pawn.attr[Combat.DAMAGE_DEAL_MULTIPLIER] ?: 1.0

    private fun getDamageTakeMultiplier(pawn: Pawn): Double = pawn.attr[Combat.DAMAGE_TAKE_MULTIPLIER] ?: 1.0

    private fun isDemon(pawn: Pawn): Boolean {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).isSpecies(NpcSpecies.DEMON)
        }
        return false
    }

    private fun isShade(pawn: Pawn): Boolean {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).isSpecies(NpcSpecies.SHADE)
        }
        return false
    }

    private fun isKalphite(pawn: Pawn): Boolean {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).isSpecies(NpcSpecies.KALPHITE)
        }
        return false
    }

    private fun isScarab(pawn: Pawn): Boolean {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).isSpecies(NpcSpecies.SCARAB)
        }
        return false
    }

    private fun isWearingDharok(pawn: Pawn): Boolean {
        if (pawn.entityType.isPlayer) {
            val player = pawn as Player
            return player.hasEquipped(Wearpos.Hat, "items.barrows_dharok_head", "items.barrows_dharok_head_25", "items.barrows_dharok_head_50", "items.barrows_dharok_head_75", "items.barrows_dharok_head_100")
                    && player.hasEquipped(Wearpos.RightHand, "items.barrows_dharok_weapon", "items.barrows_dharok_weapon_25", "items.barrows_dharok_weapon_50", "items.barrows_dharok_weapon_75", "items.barrows_dharok_weapon_100")
                    && player.hasEquipped(Wearpos.Torso, "items.barrows_dharok_body", "items.barrows_dharok_body_25", "items.barrows_dharok_body_50", "items.barrows_dharok_body_75", "items.barrows_dharok_body_100")
                    && player.hasEquipped(Wearpos.Legs, "items.barrows_dharok_legs", "items.barrows_dharok_legs_25", "items.barrows_dharok_legs_50", "items.barrows_dharok_legs_75", "items.barrows_dharok_legs_100")
        }
        return false
    }

    private fun isWearingVerac(pawn: Pawn): Boolean {
        if (pawn.entityType.isPlayer) {
            val player = pawn as Player
            return player.hasEquipped(Wearpos.Hat, "items.barrows_verac_head", "items.barrows_verac_head_25", "items.barrows_verac_head_50", "items.barrows_verac_head_75", "items.barrows_verac_head_100")
                    && player.hasEquipped(Wearpos.RightHand, "items.barrows_verac_weapon", "items.barrows_verac_weapon_25", "items.barrows_verac_weapon_50", "items.barrows_verac_weapon_75", "items.barrows_verac_weapon_100")
                    && player.hasEquipped(Wearpos.Torso, "items.barrows_verac_body", "items.barrows_verac_body_25", "items.barrows_verac_body_50", "items.barrows_verac_body_75", "items.barrows_verac_body_100")
                    && player.hasEquipped(Wearpos.Legs, "items.barrows_verac_legs", "items.barrows_verac_legs_25", "items.barrows_verac_legs_50", "items.barrows_verac_legs_75", "items.barrows_verac_legs_100")
        }
        return false
    }

    private fun isWearingTorag(player: Player): Boolean {
        return player.hasEquipped(Wearpos.Hat, "items.barrows_torag_head", "items.barrows_torag_head_25", "items.barrows_torag_head_50", "items.barrows_torag_head_75", "items.barrows_torag_head_100")
                && player.hasEquipped(Wearpos.RightHand, "items.barrows_torag_weapon", "items.barrows_torag_weapon_25", "items.barrows_torag_weapon_50", "items.barrows_torag_weapon_75", "items.barrows_torag_weapon_100")
                && player.hasEquipped(Wearpos.Torso, "items.barrows_torag_body", "items.barrows_torag_body_25", "items.barrows_torag_body_50", "items.barrows_torag_body_75", "items.barrows_torag_body_100")
                && player.hasEquipped(Wearpos.Legs, "items.barrows_torag_legs", "items.barrows_torag_legs_25", "items.barrows_torag_legs_50", "items.barrows_torag_legs_75", "items.barrows_torag_legs_100")
    }
}
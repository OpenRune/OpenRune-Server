package org.alter.combat.formula

import org.alter.api.BonusSlot
import org.alter.api.EquipmentType
import org.alter.api.NpcSpecies
import org.alter.api.PrayerIcon
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getBonus
import org.alter.api.ext.getStrengthBonus
import org.alter.api.ext.getVarbit
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasPrayerIcon
import org.alter.api.ext.hasWeaponType
import org.alter.api.ext.isSpecies
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.isMelee
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent

/**
 * Event-based melee combat formula plugin.
 *
 * Ported from [org.alter.plugins.content.combat.formula.MeleeCombatFormula].
 * Registers at priority 0 so higher-priority listeners can override or modify results.
 */
class MeleeCombatFormulaPlugin {

    companion object {
        // TODO: These AttributeKey instances are NOT the same objects as Combat.DRAGON_BATTLEAXE_BONUS,
        //  Combat.DAMAGE_DEAL_MULTIPLIER, Combat.DAMAGE_TAKE_MULTIPLIER in game-plugins.
        //  Until those keys are moved to a shared module (game-server or game-api), attribute lookups
        //  using these keys will return null and fall back to defaults (0.0 / 1.0).
        //  This is functionally correct for most scenarios but means dragon battleaxe spec and
        //  custom damage multipliers won't apply until the keys are unified.
        private val DRAGON_BATTLEAXE_BONUS = AttributeKey<Double>()
        private val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
        private val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()

        private val MELEE_VOID = arrayOf(
            "items.game_pest_melee_helm",
            "items.pest_void_knight_top",
            "items.pest_void_knight_robes",
            "items.pest_void_knight_gloves"
        )

        private val MELEE_ELITE_VOID = arrayOf(
            "items.game_pest_melee_helm",
            "items.elite_void_knight_top",
            "items.elite_void_knight_robes",
            "items.pest_void_knight_gloves"
        )

        private val BLACK_MASKS = arrayOf(
            "items.harmless_black_mask",
            "items.harmless_black_mask_1", "items.harmless_black_mask_2", "items.harmless_black_mask_3",
            "items.harmless_black_mask_4", "items.harmless_black_mask_5", "items.harmless_black_mask_6",
            "items.harmless_black_mask_7", "items.harmless_black_mask_8", "items.harmless_black_mask_9",
            "items.harmless_black_mask_10"
        )

        private val BLACK_MASKS_I = arrayOf(
            "items.nzone_black_mask",
            "items.nzone_black_mask_1", "items.nzone_black_mask_2", "items.nzone_black_mask_3",
            "items.nzone_black_mask_4", "items.nzone_black_mask_5", "items.nzone_black_mask_6",
            "items.nzone_black_mask_7", "items.nzone_black_mask_8", "items.nzone_black_mask_9",
            "items.nzone_black_mask_10"
        )

        // Prayer varbit names (from Prayer enum in game-plugins)
        private const val VARBIT_BURST_OF_STRENGTH = "varbits.prayer_burstofstrength"
        private const val VARBIT_SUPERHUMAN_STRENGTH = "varbits.prayer_superhumanstrength"
        private const val VARBIT_ULTIMATE_STRENGTH = "varbits.prayer_ultimatestrength"
        private const val VARBIT_CHIVALRY = "varbits.prayer_chivalry"
        private const val VARBIT_PIETY = "varbits.prayer_piety"

        private const val VARBIT_CLARITY_OF_THOUGHT = "varbits.prayer_clarityofthought"
        private const val VARBIT_IMPROVED_REFLEXES = "varbits.prayer_improvedreflexes"
        private const val VARBIT_INCREDIBLE_REFLEXES = "varbits.prayer_incrediblereflexes"

        private const val VARBIT_THICK_SKIN = "varbits.prayer_thickskin"
        private const val VARBIT_ROCK_SKIN = "varbits.prayer_rockskin"
        private const val VARBIT_STEEL_SKIN = "varbits.prayer_steelskin"
        private const val VARBIT_RIGOUR = "varbits.prayer_rigour"
        private const val VARBIT_AUGURY = "varbits.prayer_augury"
    }

    fun register() {
        EventListener.on<AccuracyRollEvent> {
            where { combatStyle.isMelee() }
            priority(0)
            then {
                attackRoll = calculateAttackRoll(attacker, target, combatStyle)
                defenceRoll = calculateDefenceRoll(attacker, target, combatStyle)
            }
        }

        EventListener.on<MaxHitRollEvent> {
            where { combatStyle.isMelee() }
            priority(0)
            then {
                maxHit = calculateMaxHit(attacker, target)
            }
        }
    }

    // ========================================================================
    // Attack roll
    // ========================================================================

    private fun calculateAttackRoll(attacker: Pawn, target: Pawn, combatStyle: CombatStyle): Int {
        val effectiveLevel = when (attacker) {
            is Player -> getEffectiveAttackLevel(attacker)
            is Npc -> getEffectiveAttackLevel(attacker)
            else -> 0.0
        }
        val equipmentBonus = getEquipmentAttackBonus(attacker, combatStyle)

        var maxRoll = effectiveLevel * (equipmentBonus + 64.0)
        if (attacker is Player) {
            maxRoll = applyAttackSpecials(attacker, target, maxRoll)
        }
        return maxRoll.toInt()
    }

    // ========================================================================
    // Defence roll
    // ========================================================================

    private fun calculateDefenceRoll(attacker: Pawn, target: Pawn, combatStyle: CombatStyle): Int {
        val effectiveLevel = when (target) {
            is Player -> getEffectiveDefenceLevel(target)
            is Npc -> getEffectiveDefenceLevel(target)
            else -> 0.0
        }
        val defenceBonus = getEquipmentDefenceBonus(target, combatStyle)

        var maxRoll = effectiveLevel * (defenceBonus + 64.0)
        maxRoll = applyDefenceSpecials(target, maxRoll)
        return maxRoll.toInt()
    }

    // ========================================================================
    // Max hit
    // ========================================================================

    private fun calculateMaxHit(attacker: Pawn, target: Pawn): Int {
        val effectiveStr = when (attacker) {
            is Player -> getEffectiveStrengthLevel(attacker)
            is Npc -> getEffectiveStrengthLevel(attacker)
            else -> 0.0
        }
        val strBonus = getEquipmentStrengthBonus(attacker)

        var base = Math.floor(0.5 + effectiveStr * (strBonus + 64.0) / 640.0).toInt()
        if (attacker is Player) {
            base = applyStrengthSpecials(attacker, target, base)
        }
        return base
    }

    // ========================================================================
    // Effective levels - Player
    // ========================================================================

    private fun getEffectiveStrengthLevel(player: Player): Double {
        val baseLevel = player.getSkills().getBaseLevel(Skills.STRENGTH).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.STRENGTH).toDouble()
        var potionBonus = Math.max(0.0, currentLevel - baseLevel)

        // Dragon battleaxe special attack bonus
        val dragonBattleaxeBonus = player.attr[DRAGON_BATTLEAXE_BONUS] ?: 0.0
        if (dragonBattleaxeBonus > 0.0) {
            potionBonus += dragonBattleaxeBonus
        }

        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        // Prayer multiplier
        effectiveLevel = Math.floor(effectiveLevel * getPrayerStrengthMultiplier(player))

        // Style bonus
        effectiveLevel += when (getAttackStyle(player)) {
            AttackStyle.AGGRESSIVE -> 3.0
            AttackStyle.CONTROLLED -> 1.0
            else -> 0.0
        }

        effectiveLevel += 8.0

        // Void bonus
        if (player.hasEquipped(MELEE_VOID) || player.hasEquipped(MELEE_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.10)
        }

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveAttackLevel(player: Player): Double {
        val baseLevel = player.getSkills().getBaseLevel(Skills.ATTACK).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.ATTACK).toDouble()
        val potionBonus = Math.max(0.0, currentLevel - baseLevel)

        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        effectiveLevel = Math.floor(effectiveLevel * getPrayerAttackMultiplier(player))

        effectiveLevel += when (getAttackStyle(player)) {
            AttackStyle.ACCURATE -> 3.0
            AttackStyle.CONTROLLED -> 1.0
            else -> 0.0
        }

        effectiveLevel += 8.0

        if (player.hasEquipped(MELEE_VOID) || player.hasEquipped(MELEE_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.10)
        }

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveDefenceLevel(player: Player): Double {
        val baseLevel = player.getSkills().getBaseLevel(Skills.DEFENCE).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.DEFENCE).toDouble()
        val potionBonus = Math.max(0.0, currentLevel - baseLevel)

        var effectiveLevel = Math.floor(baseLevel + potionBonus)

        effectiveLevel = Math.floor(effectiveLevel * getPrayerDefenceMultiplier(player))

        effectiveLevel += when (getAttackStyle(player)) {
            AttackStyle.DEFENSIVE -> 3.0
            AttackStyle.CONTROLLED -> 1.0
            AttackStyle.LONG_RANGE -> 3.0
            else -> 0.0
        }

        effectiveLevel += 8.0

        return Math.floor(effectiveLevel)
    }

    // ========================================================================
    // Effective levels - NPC
    // ========================================================================

    private fun getEffectiveStrengthLevel(npc: Npc): Double {
        return npc.combatDef.strength.toDouble() + 8.0
    }

    private fun getEffectiveAttackLevel(npc: Npc): Double {
        return npc.combatDef.attack.toDouble() + 8.0
    }

    private fun getEffectiveDefenceLevel(npc: Npc): Double {
        return npc.combatDef.defence.toDouble() + 8.0
    }

    // ========================================================================
    // Equipment bonuses
    // ========================================================================

    private fun getEquipmentStrengthBonus(pawn: Pawn): Double = when (pawn) {
        is Player -> pawn.getStrengthBonus().toDouble()
        is Npc -> pawn.getStrengthBonus().toDouble()
        else -> throw IllegalArgumentException("Invalid pawn type: $pawn")
    }

    private fun getEquipmentAttackBonus(pawn: Pawn, combatStyle: CombatStyle): Double {
        val bonus = when (combatStyle) {
            CombatStyle.STAB -> BonusSlot.ATTACK_STAB
            CombatStyle.SLASH -> BonusSlot.ATTACK_SLASH
            CombatStyle.CRUSH -> BonusSlot.ATTACK_CRUSH
            else -> throw IllegalStateException("Invalid melee combat style: $combatStyle")
        }
        return pawn.getBonus(bonus).toDouble()
    }

    private fun getEquipmentDefenceBonus(pawn: Pawn, combatStyle: CombatStyle): Double {
        val bonus = when (combatStyle) {
            CombatStyle.STAB -> BonusSlot.DEFENCE_STAB
            CombatStyle.SLASH -> BonusSlot.DEFENCE_SLASH
            CombatStyle.CRUSH -> BonusSlot.DEFENCE_CRUSH
            else -> throw IllegalStateException("Invalid melee combat style: $combatStyle")
        }
        return pawn.getBonus(bonus).toDouble()
    }

    // ========================================================================
    // Prayer multipliers (inlined — reads varbits directly)
    // ========================================================================

    private fun isPrayerActive(player: Player, varbit: String): Boolean {
        return player.getVarbit(varbit) != 0
    }

    private fun getPrayerStrengthMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_BURST_OF_STRENGTH) -> 1.05
        isPrayerActive(player, VARBIT_SUPERHUMAN_STRENGTH) -> 1.10
        isPrayerActive(player, VARBIT_ULTIMATE_STRENGTH) -> 1.15
        isPrayerActive(player, VARBIT_CHIVALRY) -> 1.18
        isPrayerActive(player, VARBIT_PIETY) -> 1.23
        else -> 1.0
    }

    private fun getPrayerAttackMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_CLARITY_OF_THOUGHT) -> 1.05
        isPrayerActive(player, VARBIT_IMPROVED_REFLEXES) -> 1.10
        isPrayerActive(player, VARBIT_INCREDIBLE_REFLEXES) -> 1.15
        isPrayerActive(player, VARBIT_CHIVALRY) -> 1.15
        isPrayerActive(player, VARBIT_PIETY) -> 1.20
        else -> 1.0
    }

    private fun getPrayerDefenceMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_THICK_SKIN) -> 1.05
        isPrayerActive(player, VARBIT_ROCK_SKIN) -> 1.10
        isPrayerActive(player, VARBIT_STEEL_SKIN) -> 1.15
        isPrayerActive(player, VARBIT_CHIVALRY) -> 1.20
        isPrayerActive(player, VARBIT_PIETY) -> 1.25
        isPrayerActive(player, VARBIT_RIGOUR) -> 1.25
        isPrayerActive(player, VARBIT_AUGURY) -> 1.25
        else -> 1.0
    }

    // ========================================================================
    // Attack style resolution (inlined from CombatConfigs.getAttackStyle)
    // ========================================================================

    private fun getAttackStyle(player: Player): AttackStyle {
        val style = player.getAttackStyle()

        return when {
            player.hasWeaponType(WeaponType.NONE) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.AGGRESSIVE
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.RAPID
                3 -> AttackStyle.LONG_RANGE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.TRIDENT) -> when (style) {
                0, 1 -> AttackStyle.ACCURATE
                3 -> AttackStyle.LONG_RANGE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(
                WeaponType.AXE, WeaponType.HAMMER, WeaponType.TWO_HANDED,
                WeaponType.PICKAXE, WeaponType.DAGGER, WeaponType.MAGIC_STAFF,
                WeaponType.LONG_SWORD, WeaponType.CLAWS
            ) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.AGGRESSIVE
                2 -> AttackStyle.CONTROLLED
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.SPEAR) -> when (style) {
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.CONTROLLED
            }

            player.hasWeaponType(WeaponType.HALBERD) -> when (style) {
                0 -> AttackStyle.CONTROLLED
                1 -> AttackStyle.AGGRESSIVE
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.SCYTHE) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1, 2 -> AttackStyle.AGGRESSIVE
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.WHIP) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.CONTROLLED
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.BLUDGEON) -> when (style) {
                0, 1, 3 -> AttackStyle.AGGRESSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.BULWARK) -> AttackStyle.ACCURATE

            else -> AttackStyle.NONE
        }
    }

    // ========================================================================
    // Strength specials (max hit modifiers)
    // ========================================================================

    private fun applyStrengthSpecials(player: Player, target: Pawn, base: Int): Int {
        var hit = base.toDouble()

        // Equipment multiplier (salve amulet variants)
        hit *= getEquipmentMultiplier(player)
        hit = Math.floor(hit)

        // NOTE: specialAttackMultiplier is not passed through events yet.
        // Special attacks will be handled by separate event listeners in Layer 4.
        // For now, base formula assumes multiplier of 1.0 (no special attack).

        // Protection prayers: 100% reduction in PvM, 40% reduction in PvP
        if (target.hasPrayerIcon(PrayerIcon.PROTECT_FROM_MELEE)) {
            if (target.entityType.isNpc) {
                hit = 0.0
            } else {
                hit *= 0.6
                hit = Math.floor(hit)
            }
        }

        // Passive multipliers (Dharok, berserker necklace, etc.)
        hit = applyPassiveMultiplier(player, target, hit)
        hit = Math.floor(hit)

        // Damage deal multiplier
        hit *= getDamageDealMultiplier(player)
        hit = Math.floor(hit)

        // Damage take multiplier
        hit *= getDamageTakeMultiplier(target)
        hit = Math.floor(hit)

        return hit.toInt()
    }

    // ========================================================================
    // Attack specials (accuracy modifiers)
    // ========================================================================

    private fun applyAttackSpecials(player: Player, target: Pawn, base: Double): Double {
        var hit = base

        // Equipment multiplier (salve amulet variants)
        hit *= getEquipmentMultiplier(player)
        hit = Math.floor(hit)

        // Slayer helmet / black mask accuracy bonus
        hit *= getSlayerHelmetMultiplier(player, target)
        hit = Math.floor(hit)

        // Arclight vs demons
        if (player.hasEquipped(EquipmentType.WEAPON, "items.arclight") && isDemon(target)) {
            hit *= 1.7
        }
        hit = Math.floor(hit)

        return hit
    }

    // ========================================================================
    // Defence specials
    // ========================================================================

    private fun applyDefenceSpecials(target: Pawn, base: Double): Double {
        var hit = base

        if (target is Player && isWearingTorag(target) && target.hasEquipped(EquipmentType.AMULET, "items.damned_amulet")) {
            val lost = (target.getMaxHp() - target.getCurrentHp()) / 100.0
            val max = target.getMaxHp() / 100.0
            hit *= (1.0 + (lost * max))
            hit = Math.floor(hit)
        }

        return hit
    }

    // ========================================================================
    // Equipment and passive multipliers
    // ========================================================================

    private fun getEquipmentMultiplier(player: Player): Double = when {
        player.hasEquipped(EquipmentType.AMULET, "items.crystalshard_necklace") -> 7.0 / 6.0
        player.hasEquipped(EquipmentType.AMULET, "items.lotr_crystalshard_necklace_upgrade") -> 1.2
        else -> 1.0
    }

    /**
     * Slayer helmet / black mask multiplier.
     * Only applies when on a slayer task for the target NPC.
     */
    private fun getSlayerHelmetMultiplier(player: Player, target: Pawn): Double {
        if (!hasSlayerTaskBonus(player, target)) {
            return 1.0
        }
        return when {
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS) ||
                player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS_I) -> 7.0 / 6.0
            else -> 1.0
        }
    }

    /**
     * TODO: Implement slayer task system check.
     * Currently always returns false (no slayer bonus applied).
     */
    private fun hasSlayerTaskBonus(player: Player, target: Pawn): Boolean {
        return false
    }

    private fun applyPassiveMultiplier(pawn: Player, target: Pawn, base: Double): Double {
        val world = pawn.world
        var multiplier = when {
            pawn.hasEquipped(EquipmentType.AMULET, "items.jewl_beserker_necklace") -> 1.2
            isWearingDharok(pawn) -> {
                val lost = (pawn.getMaxHp() - pawn.getCurrentHp()) / 100.0
                val max = pawn.getMaxHp() / 100.0
                1.0 + (lost * max)
            }
            pawn.hasEquipped(EquipmentType.WEAPON, "items.gadderanks_warhammer") && isShade(target) ->
                if (world.chance(1, 20)) 2.0 else 1.25
            pawn.hasEquipped(EquipmentType.WEAPON, "items.contact_keris", "items.contact_keris_p") && (isKalphite(target) || isScarab(target)) ->
                if (world.chance(1, 51)) 3.0 else (4.0 / 3.0)
            else -> 1.0
        }

        // Slayer helmet / black mask bonus for damage
        multiplier *= getSlayerHelmetMultiplier(pawn, target)

        if (multiplier == 1.0 && isWearingVerac(pawn)) {
            return base + 1.0
        }
        return base * multiplier
    }

    private fun getDamageDealMultiplier(pawn: Pawn): Double = pawn.attr[DAMAGE_DEAL_MULTIPLIER] ?: 1.0

    private fun getDamageTakeMultiplier(pawn: Pawn): Double = pawn.attr[DAMAGE_TAKE_MULTIPLIER] ?: 1.0

    // ========================================================================
    // NPC species checks
    // ========================================================================

    private fun isDemon(pawn: Pawn): Boolean =
        pawn.entityType.isNpc && (pawn as Npc).isSpecies(NpcSpecies.DEMON)

    private fun isShade(pawn: Pawn): Boolean =
        pawn.entityType.isNpc && (pawn as Npc).isSpecies(NpcSpecies.SHADE)

    private fun isKalphite(pawn: Pawn): Boolean =
        pawn.entityType.isNpc && (pawn as Npc).isSpecies(NpcSpecies.KALPHITE)

    private fun isScarab(pawn: Pawn): Boolean =
        pawn.entityType.isNpc && (pawn as Npc).isSpecies(NpcSpecies.SCARAB)

    // ========================================================================
    // Barrows set checks
    // ========================================================================

    private fun isWearingDharok(player: Player): Boolean =
        player.hasEquipped(EquipmentType.HEAD, "items.barrows_dharok_head", "items.barrows_dharok_head_25", "items.barrows_dharok_head_50", "items.barrows_dharok_head_75", "items.barrows_dharok_head_100")
            && player.hasEquipped(EquipmentType.WEAPON, "items.barrows_dharok_weapon", "items.barrows_dharok_weapon_25", "items.barrows_dharok_weapon_50", "items.barrows_dharok_weapon_75", "items.barrows_dharok_weapon_100")
            && player.hasEquipped(EquipmentType.CHEST, "items.barrows_dharok_body", "items.barrows_dharok_body_25", "items.barrows_dharok_body_50", "items.barrows_dharok_body_75", "items.barrows_dharok_body_100")
            && player.hasEquipped(EquipmentType.LEGS, "items.barrows_dharok_legs", "items.barrows_dharok_legs_25", "items.barrows_dharok_legs_50", "items.barrows_dharok_legs_75", "items.barrows_dharok_legs_100")

    private fun isWearingVerac(player: Player): Boolean =
        player.hasEquipped(EquipmentType.HEAD, "items.barrows_verac_head", "items.barrows_verac_head_25", "items.barrows_verac_head_50", "items.barrows_verac_head_75", "items.barrows_verac_head_100")
            && player.hasEquipped(EquipmentType.WEAPON, "items.barrows_verac_weapon", "items.barrows_verac_weapon_25", "items.barrows_verac_weapon_50", "items.barrows_verac_weapon_75", "items.barrows_verac_weapon_100")
            && player.hasEquipped(EquipmentType.CHEST, "items.barrows_verac_body", "items.barrows_verac_body_25", "items.barrows_verac_body_50", "items.barrows_verac_body_75", "items.barrows_verac_body_100")
            && player.hasEquipped(EquipmentType.LEGS, "items.barrows_verac_legs", "items.barrows_verac_legs_25", "items.barrows_verac_legs_50", "items.barrows_verac_legs_75", "items.barrows_verac_legs_100")

    private fun isWearingTorag(player: Player): Boolean =
        player.hasEquipped(EquipmentType.HEAD, "items.barrows_torag_head", "items.barrows_torag_head_25", "items.barrows_torag_head_50", "items.barrows_torag_head_75", "items.barrows_torag_head_100")
            && player.hasEquipped(EquipmentType.WEAPON, "items.barrows_torag_weapon", "items.barrows_torag_weapon_25", "items.barrows_torag_weapon_50", "items.barrows_torag_weapon_75", "items.barrows_torag_weapon_100")
            && player.hasEquipped(EquipmentType.CHEST, "items.barrows_torag_body", "items.barrows_torag_body_25", "items.barrows_torag_body_50", "items.barrows_torag_body_75", "items.barrows_torag_body_100")
            && player.hasEquipped(EquipmentType.LEGS, "items.barrows_torag_legs", "items.barrows_torag_legs_25", "items.barrows_torag_legs_50", "items.barrows_torag_legs_75", "items.barrows_torag_legs_100")
}

package org.alter.combat.formula

import org.alter.api.BonusSlot
import org.alter.api.EquipmentType
import org.alter.api.NpcSpecies
import org.alter.api.PrayerIcon
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getBonus
import org.alter.api.ext.getRangedStrengthBonus
import org.alter.api.ext.getVarbit
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasPrayerIcon
import org.alter.api.ext.hasWeaponType
import org.alter.api.ext.isSpecies
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.isRanged
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent

/**
 * Event-based ranged combat formula plugin.
 *
 * Ported from [org.alter.plugins.content.combat.formula.RangedCombatFormula].
 * Registers at priority 0 so higher-priority listeners can override or modify results.
 */
class RangedCombatFormulaPlugin {

    companion object {
        // TODO: These AttributeKey instances are NOT the same objects as Combat.DAMAGE_DEAL_MULTIPLIER,
        //  Combat.DAMAGE_TAKE_MULTIPLIER, and Combat.BOLT_ENCHANTMENT_EFFECT in game-plugins.
        //  Until those keys are moved to a shared module (game-server or game-api), attribute lookups
        //  using these keys will return null and fall back to defaults (1.0 / false).
        //  This is functionally correct for most scenarios but means bolt enchantment passives and
        //  custom damage multipliers won't apply until the keys are unified.
        private val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
        private val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()
        private val BOLT_ENCHANTMENT_EFFECT = AttributeKey<Boolean>()

        private val RANGED_VOID = arrayOf(
            "items.game_pest_archer_helm",
            "items.pest_void_knight_top",
            "items.pest_void_knight_robes",
            "items.pest_void_knight_gloves"
        )

        private val RANGED_ELITE_VOID = arrayOf(
            "items.game_pest_archer_helm",
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

        // Ranged prayer varbit names
        private const val VARBIT_SHARP_EYE  = "varbits.prayer_sharpeye"
        private const val VARBIT_HAWK_EYE   = "varbits.prayer_hawkeye"
        private const val VARBIT_EAGLE_EYE  = "varbits.prayer_eagleeye"
        private const val VARBIT_RIGOUR     = "varbits.prayer_rigour"

        // Defence prayer varbit names (shared with melee plugin)
        private const val VARBIT_THICK_SKIN = "varbits.prayer_thickskin"
        private const val VARBIT_ROCK_SKIN  = "varbits.prayer_rockskin"
        private const val VARBIT_STEEL_SKIN = "varbits.prayer_steelskin"
        private const val VARBIT_CHIVALRY   = "varbits.prayer_chivalry"
        private const val VARBIT_PIETY      = "varbits.prayer_piety"
        private const val VARBIT_AUGURY     = "varbits.prayer_augury"
    }

    fun register() {
        EventListener.on<AccuracyRollEvent> {
            where { combatStyle.isRanged() }
            priority(0)
            then {
                attackRoll = calculateAttackRoll(attacker, target)
                defenceRoll = calculateDefenceRoll(attacker, target)
            }
        }

        EventListener.on<MaxHitRollEvent> {
            where { combatStyle.isRanged() }
            priority(0)
            then {
                maxHit = calculateMaxHit(attacker, target)
            }
        }
    }

    // ========================================================================
    // Attack roll
    // ========================================================================

    private fun calculateAttackRoll(attacker: Pawn, target: Pawn): Int {
        val effectiveLevel = when (attacker) {
            is Player -> getEffectiveAttackLevel(attacker)
            is Npc    -> getEffectiveAttackLevel(attacker)
            else      -> 0.0
        }
        val equipmentBonus = getEquipmentAttackBonus(attacker)

        var maxRoll = effectiveLevel * (equipmentBonus + 64.0)
        if (attacker is Player) {
            maxRoll = applyAttackSpecials(attacker, target, maxRoll)
        }
        return maxRoll.toInt()
    }

    // ========================================================================
    // Defence roll
    // ========================================================================

    private fun calculateDefenceRoll(attacker: Pawn, target: Pawn): Int {
        val effectiveLevel = when (target) {
            is Player -> getEffectiveDefenceLevel(target)
            is Npc    -> getEffectiveDefenceLevel(target)
            else      -> 0.0
        }
        val defenceBonus = getEquipmentDefenceBonus(target)

        var maxRoll = effectiveLevel * (defenceBonus + 64.0)
        maxRoll = applyDefenceSpecials(target, maxRoll)
        return maxRoll.toInt()
    }

    // ========================================================================
    // Max hit
    // ========================================================================

    private fun calculateMaxHit(attacker: Pawn, target: Pawn): Int {
        val effectiveRanged = when (attacker) {
            is Player -> getEffectiveRangedLevel(attacker)
            is Npc    -> getEffectiveRangedLevel(attacker)
            else      -> 0.0
        }
        val strBonus = getEquipmentRangedStrengthBonus(attacker)

        var base = Math.floor(0.5 + effectiveRanged * (strBonus + 64.0) / 640.0).toInt()
        if (attacker is Player) {
            base = applyRangedSpecials(attacker, target, base)
        }
        return base
    }

    // ========================================================================
    // Effective levels — Player
    // ========================================================================

    /**
     * Effective ranged level used for max hit calculation.
     * Elite void gives 1.125x; regular void gives 1.10x.
     */
    private fun getEffectiveRangedLevel(player: Player): Double {
        val baseLevel    = player.getSkills().getBaseLevel(Skills.RANGED).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.RANGED).toDouble()
        val potionBonus  = Math.max(0.0, currentLevel - baseLevel)

        var effectiveLevel = Math.floor(baseLevel + potionBonus)
        effectiveLevel = Math.floor(effectiveLevel * getPrayerRangedMultiplier(player))

        effectiveLevel += when (getAttackStyle(player)) {
            AttackStyle.ACCURATE -> 3.0
            else -> 0.0
        }
        effectiveLevel += 8.0

        when {
            player.hasEquipped(RANGED_ELITE_VOID) -> effectiveLevel = Math.floor(effectiveLevel * 1.125)
            player.hasEquipped(RANGED_VOID)       -> effectiveLevel = Math.floor(effectiveLevel * 1.10)
        }

        return Math.floor(effectiveLevel)
    }

    /**
     * Effective ranged level used for attack roll calculation.
     * Both void variants give 1.10x for accuracy.
     */
    private fun getEffectiveAttackLevel(player: Player): Double {
        val baseLevel    = player.getSkills().getBaseLevel(Skills.RANGED).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.RANGED).toDouble()
        val potionBonus  = Math.max(0.0, currentLevel - baseLevel)

        var effectiveLevel = Math.floor(baseLevel + potionBonus)
        effectiveLevel = Math.floor(effectiveLevel * getPrayerAttackMultiplier(player))

        effectiveLevel += when (getAttackStyle(player)) {
            AttackStyle.ACCURATE -> 3.0
            else -> 0.0
        }
        effectiveLevel += 8.0

        if (player.hasEquipped(RANGED_VOID) || player.hasEquipped(RANGED_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.10)
        }

        return Math.floor(effectiveLevel)
    }

    private fun getEffectiveDefenceLevel(player: Player): Double {
        val baseLevel    = player.getSkills().getBaseLevel(Skills.DEFENCE).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.DEFENCE).toDouble()
        val potionBonus  = Math.max(0.0, currentLevel - baseLevel)

        var effectiveLevel = Math.floor(baseLevel + potionBonus)
        effectiveLevel = Math.floor(effectiveLevel * getPrayerDefenceMultiplier(player))

        effectiveLevel += when (getAttackStyle(player)) {
            AttackStyle.DEFENSIVE  -> 3.0
            AttackStyle.CONTROLLED -> 1.0
            AttackStyle.LONG_RANGE -> 3.0
            else -> 0.0
        }
        effectiveLevel += 8.0

        return Math.floor(effectiveLevel)
    }

    // ========================================================================
    // Effective levels — NPC
    // ========================================================================

    private fun getEffectiveRangedLevel(npc: Npc): Double = npc.combatDef.ranged.toDouble() + 8.0

    private fun getEffectiveAttackLevel(npc: Npc): Double = npc.combatDef.ranged.toDouble() + 8.0

    private fun getEffectiveDefenceLevel(npc: Npc): Double = npc.combatDef.defence.toDouble() + 8.0

    // ========================================================================
    // Equipment bonuses
    // ========================================================================

    private fun getEquipmentRangedStrengthBonus(pawn: Pawn): Double = when (pawn) {
        is Player -> pawn.getRangedStrengthBonus().toDouble()
        is Npc    -> pawn.getRangedStrengthBonus().toDouble()
        else      -> throw IllegalArgumentException("Invalid pawn type: $pawn")
    }

    private fun getEquipmentAttackBonus(pawn: Pawn): Double =
        pawn.getBonus(BonusSlot.ATTACK_RANGED).toDouble()

    private fun getEquipmentDefenceBonus(pawn: Pawn): Double =
        pawn.getBonus(BonusSlot.DEFENCE_RANGED).toDouble()

    // ========================================================================
    // Prayer multipliers (inlined — reads varbits directly)
    // ========================================================================

    private fun isPrayerActive(player: Player, varbit: String): Boolean =
        player.getVarbit(varbit) != 0

    /** Ranged strength prayer multiplier (max hit). */
    private fun getPrayerRangedMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_SHARP_EYE) -> 1.05
        isPrayerActive(player, VARBIT_HAWK_EYE)  -> 1.10
        isPrayerActive(player, VARBIT_EAGLE_EYE) -> 1.15
        isPrayerActive(player, VARBIT_RIGOUR)    -> 1.23
        else -> 1.0
    }

    /** Ranged attack prayer multiplier (accuracy roll). */
    private fun getPrayerAttackMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_SHARP_EYE) -> 1.05
        isPrayerActive(player, VARBIT_HAWK_EYE)  -> 1.10
        isPrayerActive(player, VARBIT_EAGLE_EYE) -> 1.15
        isPrayerActive(player, VARBIT_RIGOUR)    -> 1.20
        else -> 1.0
    }

    private fun getPrayerDefenceMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_THICK_SKIN) -> 1.05
        isPrayerActive(player, VARBIT_ROCK_SKIN)  -> 1.10
        isPrayerActive(player, VARBIT_STEEL_SKIN) -> 1.15
        isPrayerActive(player, VARBIT_CHIVALRY)   -> 1.20
        isPrayerActive(player, VARBIT_PIETY)      -> 1.25
        isPrayerActive(player, VARBIT_RIGOUR)     -> 1.25
        isPrayerActive(player, VARBIT_AUGURY)     -> 1.25
        else -> 1.0
    }

    // ========================================================================
    // Attack style resolution (inlined from CombatConfigs.getAttackStyle)
    // ========================================================================

    private fun getAttackStyle(player: Player): AttackStyle {
        val style = player.getAttackStyle()

        return when {
            player.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) -> when (style) {
                0    -> AttackStyle.ACCURATE
                1    -> AttackStyle.RAPID
                3    -> AttackStyle.LONG_RANGE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.NONE) -> when (style) {
                0    -> AttackStyle.ACCURATE
                1    -> AttackStyle.AGGRESSIVE
                3    -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            else -> AttackStyle.NONE
        }
    }

    // ========================================================================
    // Max hit specials
    // ========================================================================

    private fun applyRangedSpecials(player: Player, target: Pawn, base: Int): Int {
        var hit = base.toDouble()

        // Equipment multiplier (salve amulet / slayer helmet)
        hit *= getEquipmentMultiplier(player, target)
        hit = Math.floor(hit)

        // Twisted bow / dragonhunter crossbow passive scaling
        val weaponMultiplier = when {
            player.hasEquipped(EquipmentType.WEAPON, "items.dragonhunter_xbow") && isDragon(target) -> 1.3

            player.hasEquipped(EquipmentType.WEAPON, "items.twisted_bow") && target.entityType.isNpc -> {
                // TODO: cap inside Chambers of Xeric is 350
                val cap = 250.0
                val magic = when (target) {
                    is Player -> target.getSkills().getCurrentLevel(Skills.MAGIC)
                    is Npc    -> target.combatDef.magic
                    else      -> throw IllegalStateException("Invalid pawn type. [$target]")
                }
                Math.min(
                    cap,
                    250.0 + (((magic * 3.0) - 14.0) / 100.0) - (Math.pow((((magic * 3.0) / 10.0) - 140.0), 2.0) / 100.0)
                )
            }

            else -> 1.0
        }
        hit *= weaponMultiplier
        hit = Math.floor(hit)

        // Protection prayers: 100% reduction in PvM, 40% reduction in PvP
        if (target.hasPrayerIcon(PrayerIcon.PROTECT_FROM_MISSILES)) {
            if (target.entityType.isNpc) {
                hit = 0.0
            } else {
                hit *= 0.6
                hit = Math.floor(hit)
            }
        }

        // Passive multipliers (bolt enchantments)
        hit = applyPassiveMultiplier(player, target, hit)
        hit = Math.floor(hit)

        hit *= getDamageDealMultiplier(player)
        hit = Math.floor(hit)

        hit *= getDamageTakeMultiplier(target)
        hit = Math.floor(hit)

        return hit.toInt()
    }

    // ========================================================================
    // Attack roll specials
    // ========================================================================

    private fun applyAttackSpecials(player: Player, target: Pawn, base: Double): Double {
        var hit = base

        // Equipment multiplier (salve amulet / slayer helmet)
        hit *= getEquipmentMultiplier(player, target)
        hit = Math.floor(hit)

        // Twisted bow / dragonhunter crossbow passive scaling
        val weaponMultiplier = when {
            player.hasEquipped(EquipmentType.WEAPON, "items.dragonhunter_xbow") && isDragon(target) -> 1.3

            player.hasEquipped(EquipmentType.WEAPON, "items.twisted_bow") && target.entityType.isNpc -> {
                // TODO: cap inside Chambers of Xeric is 250
                val cap = 140.0
                val magic = when (target) {
                    is Player -> target.getSkills().getCurrentLevel(Skills.MAGIC)
                    is Npc    -> target.combatDef.magic
                    else      -> throw IllegalStateException("Invalid pawn type. [$target]")
                }
                Math.min(
                    cap,
                    140.0 + (((magic * 3.0) - 10.0) / 100.0) - (Math.pow((((magic * 3.0) / 10.0) - 100.0), 2.0) / 100.0)
                )
            }

            else -> 1.0
        }
        hit *= weaponMultiplier
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
            val max  = target.getMaxHp() / 100.0
            hit *= (1.0 + (lost * max))
            hit = Math.floor(hit)
        }

        return hit
    }

    // ========================================================================
    // Equipment and passive multipliers
    // ========================================================================

    private fun getEquipmentMultiplier(player: Player, target: Pawn? = null): Double = when {
        player.hasEquipped(EquipmentType.AMULET, "items.crystalshard_necklace")            -> 7.0 / 6.0
        player.hasEquipped(EquipmentType.AMULET, "items.lotr_crystalshard_necklace_upgrade") -> 1.2
        player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet")               -> 1.15
        player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet_e")             -> 1.2
        target != null && hasSlayerTaskBonus(player, target) &&
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS)   -> 7.0 / 6.0
        target != null && hasSlayerTaskBonus(player, target) &&
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS_I) -> 1.15
        else -> 1.0
    }

    /**
     * Bolt enchantment passive damage bonus.
     * Requires [BOLT_ENCHANTMENT_EFFECT] attribute to be set by the combat tick (currently stubbed).
     */
    private fun applyPassiveMultiplier(player: Player, target: Pawn, base: Double): Double {
        if (player.hasWeaponType(WeaponType.CROSSBOW) && (player.attr[BOLT_ENCHANTMENT_EFFECT] == true)) {
            val dragonstone = player.hasEquipped(
                EquipmentType.AMMO,
                "items.xbows_crossbow_bolts_runite_tipped_dragonstone",
                "items.xbows_crossbow_bolts_runite_tipped_dragonstone_enchanted",
                "items.dragon_bolts_unenchanted_dragonstone",
                "items.dragon_bolts_enchanted_dragonstone"
            )
            val opal = player.hasEquipped(
                EquipmentType.AMMO,
                "items.opal_bolt",
                "items.xbows_crossbow_bolts_bronze_tipped_opal_enchanted",
                "items.dragon_bolts_unenchanted_opal",
                "items.dragon_bolts_enchanted_opal"
            )
            val pearl = player.hasEquipped(
                EquipmentType.AMMO,
                "items.pearl_bolt",
                "items.xbows_crossbow_bolts_iron_tipped_pearl_enchanted",
                "items.dragon_bolts_unenchanted_pearl",
                "items.dragon_bolts_enchanted_pearl"
            )

            when {
                dragonstone -> return base + Math.floor(player.getSkills().getCurrentLevel(Skills.RANGED) / 5.0)
                opal        -> return base + Math.floor(player.getSkills().getCurrentLevel(Skills.RANGED) / 10.0)
                pearl       -> return base + Math.floor(
                    player.getSkills().getCurrentLevel(Skills.RANGED) / (if (isFiery(target)) 15.0 else 20.0)
                )
            }
        }
        return base
    }

    private fun getDamageDealMultiplier(pawn: Pawn): Double = pawn.attr[DAMAGE_DEAL_MULTIPLIER] ?: 1.0

    private fun getDamageTakeMultiplier(pawn: Pawn): Double = pawn.attr[DAMAGE_TAKE_MULTIPLIER] ?: 1.0

    /**
     * TODO: Implement slayer task system check.
     * Currently always returns false (no slayer bonus applied).
     */
    private fun hasSlayerTaskBonus(player: Player, target: Pawn): Boolean = false

    // ========================================================================
    // NPC species checks
    // ========================================================================

    private fun isDragon(pawn: Pawn): Boolean =
        pawn.entityType.isNpc && (pawn as Npc).isSpecies(NpcSpecies.DRACONIC)

    private fun isFiery(pawn: Pawn): Boolean =
        pawn.entityType.isNpc && (pawn as Npc).isSpecies(NpcSpecies.FIERY)

    // ========================================================================
    // Barrows set check (Torag defence bonus)
    // ========================================================================

    private fun isWearingTorag(player: Player): Boolean =
        player.hasEquipped(EquipmentType.HEAD,   "items.barrows_torag_head",   "items.barrows_torag_head_25",   "items.barrows_torag_head_50",   "items.barrows_torag_head_75",   "items.barrows_torag_head_100")
            && player.hasEquipped(EquipmentType.WEAPON, "items.barrows_torag_weapon", "items.barrows_torag_weapon_25", "items.barrows_torag_weapon_50", "items.barrows_torag_weapon_75", "items.barrows_torag_weapon_100")
            && player.hasEquipped(EquipmentType.CHEST,  "items.barrows_torag_body",   "items.barrows_torag_body_25",   "items.barrows_torag_body_50",   "items.barrows_torag_body_75",   "items.barrows_torag_body_100")
            && player.hasEquipped(EquipmentType.LEGS,   "items.barrows_torag_legs",   "items.barrows_torag_legs_25",   "items.barrows_torag_legs_50",   "items.barrows_torag_legs_75",   "items.barrows_torag_legs_100")
}

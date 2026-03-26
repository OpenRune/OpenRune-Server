package org.alter.combat.formula

import org.alter.api.BonusSlot
import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.NpcSpecies
import org.alter.api.PrayerIcon
import org.alter.api.Skills
import org.alter.api.Spellbook
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getBonus
import org.alter.api.ext.getMagicDamageBonus
import org.alter.api.ext.getVarbit
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasPrayerIcon
import org.alter.api.ext.hasSpellbook
import org.alter.api.ext.hasWeaponType
import org.alter.api.ext.isSpecies
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.isMagic
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent

/**
 * Event-based magic combat formula plugin.
 *
 * Ported from [org.alter.plugins.content.combat.formula.MagicCombatFormula].
 * Registers at priority 0 so higher-priority listeners can override or modify results.
 *
 * Magic defence differs from melee/ranged: it is split between defence level (30%)
 * and magic level (70%) rather than being purely defence-based.
 *
 * Max hit is derived from the spell's base max hit multiplied by equipment damage bonuses.
 * Void magic gives 1.45x accuracy and +0.025 damage multiplier (elite only).
 */
class MagicCombatFormulaPlugin {

    companion object {
        private val MAGE_VOID = arrayOf(
            "items.game_pest_mage_helm",
            "items.pest_void_knight_top",
            "items.pest_void_knight_robes",
            "items.pest_void_knight_gloves"
        )

        private val MAGE_ELITE_VOID = arrayOf(
            "items.game_pest_mage_helm",
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

        private val SLAYER_HELM_I = arrayOf(
            "items.nzone_slayer_helmet",
            "items.nzone_slayer_helmet_green",
            "items.nzone_slayer_helmet_red",
            "items.nzone_slayer_helmet_black",
            "items.nzone_slayer_helmet_purple",
            "items.nzone_slayer_helmet_turquoise",
            "items.nzone_slayer_helmet_hydra",
            "items.nzone_slayer_helmet_twisted"
        )

        // Magic prayer varbit names
        private const val VARBIT_MYSTIC_WILL   = "varbits.prayer_mysticwill"
        private const val VARBIT_MYSTIC_LORE   = "varbits.prayer_mysticlore"
        private const val VARBIT_MYSTIC_MIGHT  = "varbits.prayer_mysticmight"
        private const val VARBIT_AUGURY        = "varbits.prayer_augury"

        // Defence prayer varbit names (shared with melee/ranged plugins)
        private const val VARBIT_THICK_SKIN    = "varbits.prayer_thickskin"
        private const val VARBIT_ROCK_SKIN     = "varbits.prayer_rockskin"
        private const val VARBIT_STEEL_SKIN    = "varbits.prayer_steelskin"
        private const val VARBIT_CHIVALRY      = "varbits.prayer_chivalry"
        private const val VARBIT_PIETY         = "varbits.prayer_piety"
        private const val VARBIT_RIGOUR        = "varbits.prayer_rigour"
    }

    fun register() {
        EventListener.on<AccuracyRollEvent> {
            where { combatStyle.isMagic() }
            priority(0)
            then {
                attackRoll = calculateAttackRoll(attacker, target)
                defenceRoll = calculateDefenceRoll(attacker, target)
            }
        }

        EventListener.on<MaxHitRollEvent> {
            where { combatStyle.isMagic() }
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
            maxRoll = applyAttackSpecials(attacker, maxRoll)
        }
        return maxRoll.toInt()
    }

    // ========================================================================
    // Defence roll
    //
    // Magic defence = 30% defence level * (magic_defence_bonus + 64)
    //               + 70% magic level   * (magic_defence_bonus + 64)
    //
    // Both halves use the same equipment bonus (DEFENCE_MAGIC).
    // ========================================================================

    private fun calculateDefenceRoll(attacker: Pawn, target: Pawn): Int {
        return when (target) {
            is Player -> calculatePlayerDefenceRoll(target)
            is Npc    -> calculateNpcDefenceRoll(attacker, target)
            else      -> 0
        }
    }

    /**
     * Player magic defence roll.
     * Defence portion: floor(effectiveDefence * 0.3) * (bonus + 64)
     * Magic portion:   floor(magicLevel * prayerMultiplier * 0.7) * (bonus + 64)
     * Combined into one roll: (defencePart + magicPart) * (bonus + 64)
     */
    private fun calculatePlayerDefenceRoll(target: Player): Int {
        var effectiveDef = getEffectiveDefenceLevel(target)
        effectiveDef = Math.floor(effectiveDef * 0.3)

        var magicLevel = target.getSkills().getCurrentLevel(Skills.MAGIC).toDouble()
        magicLevel = Math.floor(magicLevel * getPrayerAttackMultiplier(target))
        magicLevel = Math.floor(magicLevel * 0.7)

        val combinedLevel = Math.floor(effectiveDef + magicLevel).toInt()
        val defenceBonus = getEquipmentDefenceBonus(target)
        return (combinedLevel * (defenceBonus + 64.0)).toInt()
    }

    /**
     * NPC magic defence roll.
     * Uses attacker's effective magic level against the NPC's own defence stat
     * (NPC effective magic defence = npc.combatDef.defence + 8).
     */
    private fun calculateNpcDefenceRoll(attacker: Pawn, target: Npc): Int {
        val effectiveLevel = when (attacker) {
            is Player -> getEffectiveDefenceLevel(attacker)
            is Npc    -> getEffectiveDefenceLevel(attacker)
            else      -> 0.0
        }
        val defenceBonus = getEquipmentDefenceBonus(target)
        return (effectiveLevel * (defenceBonus + 64.0)).toInt()
    }

    // ========================================================================
    // Max hit
    // ========================================================================

    private fun calculateMaxHit(attacker: Pawn, target: Pawn): Int {
        val spellBaseHit = getSpellBaseHit(attacker)

        var hit = spellBaseHit.toDouble()

        if (attacker is Player) {
            hit = applyPlayerMaxHitModifiers(attacker, target, hit)
        } else if (attacker is Npc) {
            val multiplier = 1.0 + (attacker.getMagicDamageBonus() / 100.0)
            hit = Math.floor(hit * multiplier)
        }

        hit = Math.floor(hit * getDamageDealMultiplier(attacker))
        hit = Math.floor(hit * getDamageTakeMultiplier(target))

        return hit.toInt()
    }

    /**
     * Retrieves the base max hit for the spell being cast.
     *
     * Returns the trident-formula hit if the attacker has a trident equipped.
     * For standard book spells, reads maxHit from [CombatAttributes.CASTING_SPELL] via
     * reflection (the CombatSpell enum lives in game-plugins and cannot be imported here).
     */
    private fun getSpellBaseHit(attacker: Pawn): Int {
        if (attacker is Player) {
            val magic = attacker.getSkills().getCurrentLevel(Skills.MAGIC)
            if (attacker.hasEquipped(
                    EquipmentType.WEAPON,
                    "items.tots_charged",
                    "items.tots_i_charged",
                    "items.tots"
                )
            ) {
                return (Math.floor(magic / 3.0) - 5.0).toInt().coerceAtLeast(1)
            } else if (attacker.hasEquipped(
                    EquipmentType.WEAPON,
                    "items.toxic_tots_charged",
                    "items.toxic_tots_i_charged"
                )
            ) {
                return (Math.floor(magic / 3.0) - 2.0).toInt().coerceAtLeast(1)
            }
        }
        // Standard spells — read maxHit from CASTING_SPELL via reflection
        if (attacker is Player) {
            val spell = attacker.attr[CombatAttributes.CASTING_SPELL]
            if (spell != null) {
                return try {
                    val method = spell.javaClass.getMethod("getMaxHit")
                    (method.invoke(spell) as? Number)?.toInt() ?: 1
                } catch (_: Exception) {
                    try {
                        val field = spell.javaClass.getDeclaredField("maxHit")
                        field.isAccessible = true
                        (field.get(spell) as? Number)?.toInt() ?: 1
                    } catch (_: Exception) {
                        1
                    }
                }
            }
        }
        // Fallback for NPC attacks or missing spell data
        return 1
    }

    private fun applyPlayerMaxHitModifiers(player: Player, target: Pawn, baseHit: Double): Double {
        var hit = baseHit

        // TODO: Bolt spells (+3 hit from chaos gauntlets) — requires CASTING_SPELL key unification.
        // if (player.hasEquipped(EquipmentType.GLOVES, "items.gauntlets_of_chaos") && spell in BOLT_SPELLS) {
        //     hit += 3
        // }

        // Magic damage bonus from equipment (e.g. ancestral robes, occult necklace)
        var multiplier = 1.0 + (player.getMagicDamageBonus() / 100.0)

        // Ahrim's set with amulet of the damned: 30% chance of +30% damage
        if (player.hasEquipped(EquipmentType.AMULET, "items.damned_amulet") &&
            player.hasEquipped(
                EquipmentType.WEAPON,
                "items.barrows_ahrim_weapon",
                "items.barrows_ahrim_weapon_25",
                "items.barrows_ahrim_weapon_50",
                "items.barrows_ahrim_weapon_75",
                "items.barrows_ahrim_weapon_100"
            ) &&
            player.world.chance(1, 4)
        ) {
            multiplier += 0.3
        }

        // Smoke battlestaff on standard spellbook: +10% damage
        if (player.hasEquipped(EquipmentType.WEAPON, "items.mystic_smoke_battlestaff") &&
            player.hasSpellbook(Spellbook.NORMAL)
        ) {
            multiplier += 0.1
        }

        // Void magic (elite only): +2.5% damage
        if (player.hasEquipped(MAGE_ELITE_VOID)) {
            multiplier += 0.025
        }

        hit *= multiplier
        hit = Math.floor(hit)

        // TODO: Tome of Fire bonus — disabled until charge system is implemented.
        //  Fire spells check requires CombatSpell reference (Task 6).
        //  When implemented: if (player.hasEquipped(EquipmentType.SHIELD, "items.tome_of_fire") && spell in FIRE_SPELLS && charges > 0) { hit *= 1.5 }

        // Protection prayer: 100% reduction in PvM, 40% reduction in PvP
        if (target.hasPrayerIcon(PrayerIcon.PROTECT_FROM_MAGIC)) {
            if (target.entityType.isNpc) {
                hit = 0.0
            } else {
                hit = Math.floor(hit * 0.6)
            }
        }

        // Slayer helmet (imbued) or black mask (imbued) vs slayer task target: +15%
        if (target is Npc) {
            if (hasSlayerTaskBonus(player, target) &&
                (player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS_I) ||
                    player.hasEquipped(EquipmentType.HEAD, *SLAYER_HELM_I))
            ) {
                hit = Math.floor(hit * 1.15)
            } else if (player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet_e") &&
                target.isSpecies(NpcSpecies.UNDEAD)
            ) {
                // Salve amulet (e) vs undead: +20%
                hit = Math.floor(hit * 1.20)
            }
        }

        return hit
    }

    // ========================================================================
    // Effective levels — Player
    // ========================================================================

    /**
     * Effective magic attack level.
     * OSRS formula: floor((floor(floor(Magic + Potion) * Prayer) + StyleBonus + 8) * VoidBonus)
     * Style bonus only applies when wielding a trident (accurate/controlled styles).
     * Void magic: 1.45x multiplier (both regular and elite give the same accuracy bonus).
     */
    private fun getEffectiveAttackLevel(player: Player): Double {
        val baseLevel    = player.getSkills().getBaseLevel(Skills.MAGIC).toDouble()
        val currentLevel = player.getSkills().getCurrentLevel(Skills.MAGIC).toDouble()
        val potionBonus  = Math.max(0.0, currentLevel - baseLevel)

        var effectiveLevel = Math.floor(baseLevel + potionBonus)
        effectiveLevel = Math.floor(effectiveLevel * getPrayerAttackMultiplier(player))

        // Style bonus only for trident weapons
        if (player.hasWeaponType(WeaponType.TRIDENT)) {
            effectiveLevel += when (getAttackStyle(player)) {
                AttackStyle.ACCURATE   -> 3.0
                AttackStyle.CONTROLLED -> 1.0
                else -> 0.0
            }
        }

        effectiveLevel += 8.0

        // Void magic: 1.45x accuracy multiplier
        if (player.hasEquipped(MAGE_VOID) || player.hasEquipped(MAGE_ELITE_VOID)) {
            effectiveLevel = Math.floor(effectiveLevel * 1.45)
        }

        return Math.floor(effectiveLevel)
    }

    /**
     * Effective defence level for the 30% portion of the magic defence roll.
     */
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

    private fun getEffectiveAttackLevel(npc: Npc): Double = npc.combatDef.magic.toDouble() + 8.0

    private fun getEffectiveDefenceLevel(npc: Npc): Double = npc.combatDef.defence.toDouble() + 8.0

    // ========================================================================
    // Equipment bonuses
    // ========================================================================

    private fun getEquipmentAttackBonus(pawn: Pawn): Double =
        pawn.getBonus(BonusSlot.ATTACK_MAGIC).toDouble()

    private fun getEquipmentDefenceBonus(pawn: Pawn): Double =
        pawn.getBonus(BonusSlot.DEFENCE_MAGIC).toDouble()

    // ========================================================================
    // Attack specials (accuracy modifiers)
    // ========================================================================

    private fun applyAttackSpecials(player: Player, base: Double): Double {
        var hit = base

        // Equipment multiplier (salve amulet / slayer helmet accuracy)
        hit *= getEquipmentAccuracyMultiplier(player)
        hit = Math.floor(hit)

        // Smoke battlestaff: +10% accuracy on standard spellbook
        if (player.hasEquipped(EquipmentType.WEAPON, "items.mystic_smoke_battlestaff")) {
            hit = Math.floor(hit * 1.1)
        }

        return hit
    }

    // ========================================================================
    // Equipment accuracy multipliers
    // ========================================================================

    /**
     * Equipment accuracy multiplier for magic (salve amulet variants, slayer helmet).
     * Applies to the attack roll, not max hit.
     */
    private fun getEquipmentAccuracyMultiplier(player: Player): Double = when {
        player.hasEquipped(EquipmentType.AMULET, "items.crystalshard_necklace")             -> 7.0 / 6.0
        player.hasEquipped(EquipmentType.AMULET, "items.lotr_crystalshard_necklace_upgrade") -> 1.2
        player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet")                -> 1.15
        player.hasEquipped(EquipmentType.AMULET, "items.nzone_salve_amulet_e")              -> 1.2
        hasSlayerTaskBonus(player, null) &&
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS)                            -> 7.0 / 6.0
        hasSlayerTaskBonus(player, null) &&
            player.hasEquipped(EquipmentType.HEAD, *BLACK_MASKS_I)                          -> 1.15
        else -> 1.0
    }

    // ========================================================================
    // Prayer multipliers (inlined — reads varbits directly)
    // ========================================================================

    private fun isPrayerActive(player: Player, varbit: String): Boolean =
        player.getVarbit(varbit) != 0

    /** Magic attack prayer multiplier (accuracy). */
    private fun getPrayerAttackMultiplier(player: Player): Double = when {
        isPrayerActive(player, VARBIT_MYSTIC_WILL)  -> 1.05
        isPrayerActive(player, VARBIT_MYSTIC_LORE)  -> 1.10
        isPrayerActive(player, VARBIT_MYSTIC_MIGHT) -> 1.15
        isPrayerActive(player, VARBIT_AUGURY)        -> 1.25
        else -> 1.0
    }

    /** Defence prayer multiplier (for the 30% defence portion of magic def roll). */
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
            player.hasWeaponType(WeaponType.TRIDENT) -> when (style) {
                0, 1 -> AttackStyle.ACCURATE
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
    // Damage multipliers
    // ========================================================================

    private fun getDamageDealMultiplier(pawn: Pawn): Double = pawn.attr[CombatAttributes.DAMAGE_DEAL_MULTIPLIER] ?: 1.0

    private fun getDamageTakeMultiplier(pawn: Pawn): Double = pawn.attr[CombatAttributes.DAMAGE_TAKE_MULTIPLIER] ?: 1.0

    // ========================================================================
    // Slayer task bonus
    // ========================================================================

    /**
     * TODO: Implement slayer task system check.
     * Currently always returns false (no slayer bonus applied).
     * [target] is nullable to allow reuse from the accuracy multiplier path where
     * the target is not available as a typed Npc.
     */
    private fun hasSlayerTaskBonus(player: Player, target: Npc?): Boolean = false
}

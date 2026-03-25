package org.alter.combat.strategy

import org.alter.api.EquipmentType
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getEquipment
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasWeaponType
import org.alter.game.combat.CombatStrategy
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * New-engine melee combat strategy implementing [CombatStrategy].
 *
 * Ported from [org.alter.plugins.content.combat.strategy.MeleeCombatStrategy]
 * and [org.alter.plugins.content.combat.CombatConfigs].
 *
 * Attack range, speed, and animation logic are inlined here because
 * CombatConfigs lives in game-plugins which the content module cannot import.
 */
class NewMeleeCombatStrategy : CombatStrategy {

    companion object {
        private const val DEFAULT_ATTACK_SPEED = 4
        private const val MIN_ATTACK_SPEED = 1

        private val GODSWORDS = arrayOf(
            "items.ags", "items.agsg",
            "items.bgs", "items.bgsg",
            "items.sgs", "items.sgsg",
            "items.zgs", "items.zgsg",
        )
    }

    override fun getAttackRange(attacker: Pawn): Int {
        if (attacker is Player) {
            return if (attacker.hasWeaponType(WeaponType.HALBERD)) 2 else 1
        }
        return 1
    }

    override fun getAttackSpeed(attacker: Pawn): Int {
        if (attacker is Npc) {
            return attacker.combatDef.attackSpeed
        }
        if (attacker is Player) {
            val weapon = attacker.getEquipment(EquipmentType.WEAPON) ?: return DEFAULT_ATTACK_SPEED
            return maxOf(MIN_ATTACK_SPEED, weapon.getDef().weapon!!.attackSpeed)
        }
        return DEFAULT_ATTACK_SPEED
    }

    override fun getAttackAnimation(attacker: Pawn): String {
        if (attacker is Npc) {
            return attacker.combatDef.attackAnimation
        }
        if (attacker is Player) {
            val style = attacker.getAttackStyle()
            return when {
                attacker.hasEquipped(EquipmentType.WEAPON, *GODSWORDS) -> "sequences.dh_sword_update_slash"
                attacker.hasWeaponType(WeaponType.AXE) ->
                    if (style == 1) "sequences.human_blunt_pound" else "sequences.human_axe_hack"
                attacker.hasWeaponType(WeaponType.HAMMER) -> "sequences.human_blunt_pound"
                attacker.hasWeaponType(WeaponType.BULWARK) -> "sequences.human_dinhs_bulwark_bash"
                attacker.hasWeaponType(WeaponType.SCYTHE) -> "sequences.scythe_of_vitur_attack"
                attacker.hasWeaponType(WeaponType.BOW) -> "sequences.human_bow"
                attacker.hasWeaponType(WeaponType.CROSSBOW) -> "sequences.xbows_human_fire_and_reload"
                attacker.hasWeaponType(WeaponType.LONG_SWORD) ->
                    if (style == 2) "sequences.human_sword_stab" else "sequences.human_sword_slash"
                attacker.hasWeaponType(WeaponType.TWO_HANDED) ->
                    if (style == 2) "sequences.human_dhsword_chop" else "sequences.human_dhsword_slash"
                attacker.hasWeaponType(WeaponType.PICKAXE) ->
                    if (style == 2) "sequences.human_blunt_spike" else "sequences.human_blunt_pound"
                attacker.hasWeaponType(WeaponType.DAGGER) ->
                    if (style == 2) "sequences.human_sword_slash" else "sequences.human_sword_stab"
                attacker.hasWeaponType(WeaponType.MAGIC_STAFF) || attacker.hasWeaponType(WeaponType.STAFF) ->
                    "sequences.human_stafforb_pummel"
                attacker.hasWeaponType(WeaponType.MACE) ->
                    if (style == 2) "sequences.human_blunt_spike" else "sequences.human_blunt_pound"
                attacker.hasWeaponType(WeaponType.CHINCHOMPA) -> "sequences.human_chinchompa_attack_pvn"
                attacker.hasWeaponType(WeaponType.THROWN) ->
                    if (attacker.hasEquipped(EquipmentType.WEAPON, "items.tzhaar_throwingring"))
                        "sequences.thzarr_ring_chuck_pvn"
                    else
                        "sequences.human_stake2"
                attacker.hasWeaponType(WeaponType.WHIP) -> "sequences.slayer_abyssal_whip_attack"
                attacker.hasWeaponType(WeaponType.SPEAR) || attacker.hasWeaponType(WeaponType.HALBERD) ->
                    when (style) {
                        1 -> "sequences.human_scythe_sweep"
                        2 -> "sequences.human_spear_lunge"
                        else -> "sequences.human_spear_spike"
                    }
                attacker.hasWeaponType(WeaponType.CLAWS) -> "sequences.human_axe_chop"
                else -> if (style == 1) "sequences.human_unarmedkick" else "sequences.human_unarmedpunch"
            }
        }
        // Fallback for unexpected pawn types
        return "sequences.human_unarmedpunch"
    }

    /** Melee hits land immediately — delay is 1 tick. */
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int = 1
}

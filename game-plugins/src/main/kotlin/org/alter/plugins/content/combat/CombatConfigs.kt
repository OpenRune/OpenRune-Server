package org.alter.plugins.content.combat

import org.alter.api.Wearpos
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getEquipment
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasWeaponType
import org.alter.game.model.combat.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy

/**
 * @author Tom <rspsmods@gmail.com>
 */
object CombatConfigs {
    private const val PLAYER_DEFAULT_ATTACK_SPEED = 4

    private const val MIN_ATTACK_SPEED = 1

    private val DEFENDERS =
        arrayOf(
            "items.bronze_parryingdagger",
            "items.iron_parryingdagger",
            "items.steel_parryingdagger",
            "items.mithril_parryingdagger",
            "items.black_parryingdagger",
            "items.adamant_parryingdagger",
            "items.rune_parryingdagger",
            "items.dragon_parryingdagger",
            "items.dragon_parryingdagger_t",
            "items.infernal_defender",
        )

    private val BOOKS =
        arrayOf(
            "items.saradominbook_complete",
            "items.guthixbook_complete",
            "items.zamorakbook_complete",
            "items.armadylbook_complete",
            "items.bandosbook_complete",
            "items.zarosbook_complete",
            "items.magictraining_bookofmagic",
            "items.tome_of_fire",
            "items.tome_of_fire_uncharged",
        )

    private val BOXING_GLOVES =
        arrayOf(
            "items.poh_boxing_gloves_red",
            "items.poh_boxing_gloves_blue",
            "items.beachparty_boxinggloves_yellow",
            "items.beachparty_boxinggloves_purple",
        )

    private val GODSWORDS =
        arrayOf(
            "items.ags",
            "items.agsg",
            "items.bgs",
            "items.bgsg",
            "items.sgs",
            "items.sgsg",
            "items.zgs",
            "items.zgsg",
        )

    fun getCombatStrategy(pawn: Pawn): CombatStrategy =
        when (getCombatClass(pawn)) {
            CombatClass.MELEE -> MeleeCombatStrategy
            CombatClass.MAGIC -> MagicCombatStrategy
            CombatClass.RANGED -> RangedCombatStrategy
            else -> throw IllegalStateException("Invalid combat class: ${getCombatClass(pawn)} for $pawn")
        }

    fun getCombatClass(pawn: Pawn): CombatClass {
        if (pawn is Npc) {
            return pawn.combatClass
        }

        if (pawn is Player) {
            return when {
                pawn.attr.has(Combat.CASTING_SPELL) -> CombatClass.MAGIC
                pawn.hasWeaponType(WeaponType.BOW, WeaponType.CHINCHOMPA, WeaponType.CROSSBOW, WeaponType.THROWN) -> CombatClass.RANGED
                else -> CombatClass.MELEE
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getAttackDelay(pawn: Pawn): Int {
        if (pawn is Npc) {
            return pawn.combatDef.attackSpeed
        }

        if (pawn is Player) {
            val default = PLAYER_DEFAULT_ATTACK_SPEED
            val weapon = pawn.getEquipment(Wearpos.RightHand) ?: return default
            return Math.max(MIN_ATTACK_SPEED, weapon.getDef().weapon!!.attackSpeed)
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getCombatDef(pawn: Pawn): NpcCombatDef? {
        if (pawn is Npc) {
            return pawn.combatDef
        }
        return null
    }

    fun getAttackAnimation(pawn: Pawn): String {
        if (pawn is Npc) {
            return pawn.combatDef.attackAnimation
        }

        if (pawn is Player) {
            val style = pawn.getAttackStyle()

            return when {
                pawn.hasEquipped(Wearpos.RightHand, *GODSWORDS) -> "sequences.dh_sword_update_slash"
                pawn.hasWeaponType(WeaponType.AXE) -> if (style == 1) "sequences.human_blunt_pound" else "sequences.human_axe_hack"
                pawn.hasWeaponType(WeaponType.HAMMER) -> "sequences.human_blunt_pound"
                pawn.hasWeaponType(WeaponType.BULWARK) -> "sequences.human_dinhs_bulwark_bash"
                pawn.hasWeaponType(WeaponType.SCYTHE) -> "sequences.scythe_of_vitur_attack"
                pawn.hasWeaponType(WeaponType.BOW) -> "sequences.human_bow"
                pawn.hasWeaponType(WeaponType.CROSSBOW) -> "sequences.xbows_human_fire_and_reload"
                pawn.hasWeaponType(WeaponType.LONG_SWORD) -> if (style == 2) "sequences.human_sword_stab" else "sequences.human_sword_slash"
                pawn.hasWeaponType(WeaponType.TWO_HANDED) -> if (style == 2) "sequences.human_dhsword_chop" else "sequences.human_dhsword_slash"
                pawn.hasWeaponType(WeaponType.PICKAXE) -> if (style == 2) "sequences.human_blunt_spike" else "sequences.human_blunt_pound"
                pawn.hasWeaponType(WeaponType.DAGGER) -> if (style == 2) "sequences.human_sword_slash" else "sequences.human_sword_stab"
                pawn.hasWeaponType(WeaponType.MAGIC_STAFF) || pawn.hasWeaponType(WeaponType.STAFF) -> "sequences.human_stafforb_pummel"
                pawn.hasWeaponType(WeaponType.MACE) -> if (style == 2) "sequences.human_blunt_spike" else "sequences.human_blunt_pound"
                pawn.hasWeaponType(WeaponType.CHINCHOMPA) -> "sequences.human_chinchompa_attack_pvn"
                pawn.hasWeaponType(WeaponType.THROWN) -> if (pawn.hasEquipped(Wearpos.RightHand, "items.tzhaar_throwingring")) "sequences.thzarr_ring_chuck_pvn" else "sequences.human_stake2"
                pawn.hasWeaponType(WeaponType.WHIP) -> "sequences.slayer_abyssal_whip_attack"
                pawn.hasWeaponType(WeaponType.SPEAR) || pawn.hasWeaponType(WeaponType.HALBERD) ->
                    if (style == 1) {
                        "sequences.human_scythe_sweep"
                    } else if (style == 2) {
                        "sequences.human_spear_lunge"
                    } else {
                        "sequences.human_spear_spike"
                    }
                pawn.hasWeaponType(WeaponType.CLAWS) -> "sequences.human_axe_chop"
                else -> if (style == 1) "sequences.human_unarmedkick" else "sequences.human_unarmedpunch"
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getBlockAnimation(pawn: Pawn): String {
        if (pawn is Npc) {
            return pawn.combatDef.blockAnimation
        }

        if (pawn is Player) {
            return when {
                pawn.hasEquipped(Wearpos.LeftHand, *BOOKS) -> "sequences.human_stafforb_block"
                pawn.hasEquipped(Wearpos.RightHand, "items.trollromance_toboggon_waxed") -> "sequences.trollromance_toboggan_defend"
                pawn.hasEquipped(Wearpos.RightHand, "items.easter_basket_2005") -> "sequences.human_rubber_chicken_block"
                pawn.hasEquipped(Wearpos.LeftHand, *DEFENDERS) -> "sequences.warguild_parry_defend"
                pawn.getEquipment(Wearpos.LeftHand) != null -> "sequences.human_shield_defence" // If wearing any shield, this animation is used

                pawn.hasEquipped(Wearpos.RightHand, *BOXING_GLOVES) -> "sequences.human_boxing_block"
                pawn.hasEquipped(Wearpos.RightHand, *GODSWORDS) -> "sequences.dh_sword_update_defend"
                pawn.hasEquipped(Wearpos.RightHand, "items.light_ballista", "items.heavy_ballista") -> "sequences.ballista_defend"
                pawn.hasEquipped(Wearpos.RightHand, "items.zamorak_spear") -> "sequences.human_zamorakspear_block"

                pawn.hasWeaponType(WeaponType.DAGGER) -> "sequences.human_ddagger_block"
                pawn.hasWeaponType(WeaponType.LONG_SWORD) -> "human_sword_def"
                pawn.hasWeaponType(WeaponType.PICKAXE, WeaponType.CLAWS) -> "sequences.human_axe_block"
                pawn.hasWeaponType(WeaponType.MACE) -> "sequences.human_blunt_block"
                pawn.hasWeaponType(WeaponType.TWO_HANDED) -> "sequences.human_dhsword_block"
                pawn.hasWeaponType(WeaponType.MAGIC_STAFF) -> "sequences.human_stafforb_block"
                pawn.hasWeaponType(WeaponType.BOW) -> "sequences.human_unarmedblock"
                pawn.hasWeaponType(WeaponType.SPEAR, WeaponType.HALBERD) -> "sequences.human_spear_block"
                pawn.hasWeaponType(WeaponType.WHIP) -> "sequences.slayer_abyssal_whip_defend"
                pawn.hasWeaponType(WeaponType.BULWARK) -> "sequences.human_dinhs_bulwark_block"
                else -> "sequences.human_unarmedblock"
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getCombatStyle(pawn: Pawn): CombatStyle {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).combatStyle
        }

        if (pawn is Player) {
            val style = pawn.getAttackStyle()

            return when {
                pawn.attr.has(Combat.CASTING_SPELL) -> CombatStyle.MAGIC

                pawn.hasWeaponType(WeaponType.NONE) ->
                    when (style) {
                        0 -> CombatStyle.CRUSH
                        1 -> CombatStyle.CRUSH
                        3 -> CombatStyle.CRUSH
                        else -> CombatStyle.NONE
                    }

                pawn.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) -> CombatStyle.RANGED

                pawn.hasWeaponType(WeaponType.AXE) ->
                    when (style) {
                        2 -> CombatStyle.CRUSH
                        else -> CombatStyle.SLASH
                    }

                pawn.hasWeaponType(WeaponType.HAMMER) -> CombatStyle.CRUSH

                pawn.hasWeaponType(WeaponType.CLAWS) ->
                    when (style) {
                        2 -> CombatStyle.STAB
                        else -> CombatStyle.SLASH
                    }

                pawn.hasWeaponType(WeaponType.SALAMANDER) ->
                    when (style) {
                        0 -> CombatStyle.SLASH
                        1 -> CombatStyle.RANGED
                        else -> CombatStyle.MAGIC
                    }

                pawn.hasWeaponType(WeaponType.LONG_SWORD) ->
                    when (style) {
                        2 -> CombatStyle.STAB
                        else -> CombatStyle.SLASH
                    }

                pawn.hasWeaponType(WeaponType.TWO_HANDED) ->
                    when (style) {
                        2 -> CombatStyle.CRUSH
                        else -> CombatStyle.SLASH
                    }

                pawn.hasWeaponType(WeaponType.PICKAXE) ->
                    when (style) {
                        2 -> CombatStyle.CRUSH
                        else -> CombatStyle.STAB
                    }

                pawn.hasWeaponType(WeaponType.HALBERD) ->
                    when (style) {
                        1 -> CombatStyle.SLASH
                        else -> CombatStyle.STAB
                    }

                pawn.hasWeaponType(WeaponType.STAFF) -> CombatStyle.CRUSH

                pawn.hasWeaponType(WeaponType.SCYTHE) ->
                    when (style) {
                        2 -> CombatStyle.CRUSH
                        else -> CombatStyle.SLASH
                    }

                pawn.hasWeaponType(WeaponType.SPEAR) ->
                    when (style) {
                        1 -> CombatStyle.SLASH
                        2 -> CombatStyle.CRUSH
                        else -> CombatStyle.STAB
                    }

                pawn.hasWeaponType(WeaponType.MACE) ->
                    when (style) {
                        2 -> CombatStyle.STAB
                        else -> CombatStyle.CRUSH
                    }

                pawn.hasWeaponType(WeaponType.DAGGER) ->
                    when (style) {
                        2 -> CombatStyle.SLASH
                        else -> CombatStyle.STAB
                    }

                pawn.hasWeaponType(WeaponType.MAGIC_STAFF) -> CombatStyle.CRUSH

                pawn.hasWeaponType(WeaponType.WHIP) -> CombatStyle.SLASH

                pawn.hasWeaponType(WeaponType.STAFF_HALBERD) ->
                    when (style) {
                        0 -> CombatStyle.STAB
                        1 -> CombatStyle.SLASH
                        else -> CombatStyle.CRUSH
                    }

                pawn.hasWeaponType(WeaponType.TRIDENT) -> CombatStyle.MAGIC

                pawn.hasWeaponType(WeaponType.BLUDGEON) -> CombatStyle.CRUSH

                pawn.hasWeaponType(WeaponType.BULWARK) ->
                    when (style) {
                        0 -> CombatStyle.CRUSH
                        else -> CombatStyle.NONE
                    }

                else -> CombatStyle.NONE
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getXpMode(player: Player): XpMode {
        val style = player.getAttackStyle()

        return when {
            player.hasWeaponType(WeaponType.NONE) -> {
                when (style) {
                    1 -> XpMode.STRENGTH
                    3 -> XpMode.DEFENCE
                    else -> XpMode.ATTACK
                }
            }

            player.hasWeaponType(
                WeaponType.AXE,
                WeaponType.HAMMER,
                WeaponType.TWO_HANDED,
                WeaponType.PICKAXE,
                WeaponType.DAGGER,
                WeaponType.STAFF,
                WeaponType.MAGIC_STAFF,
            ) -> {
                when (style) {
                    1 -> XpMode.STRENGTH
                    2 -> XpMode.STRENGTH
                    3 -> XpMode.DEFENCE
                    else -> XpMode.ATTACK
                }
            }

            player.hasWeaponType(WeaponType.LONG_SWORD, WeaponType.MACE, WeaponType.CLAWS) -> {
                when (style) {
                    1 -> XpMode.STRENGTH
                    2 -> XpMode.SHARED
                    3 -> XpMode.DEFENCE
                    else -> XpMode.ATTACK
                }
            }

            player.hasWeaponType(WeaponType.WHIP) -> {
                when (style) {
                    1 -> XpMode.SHARED
                    3 -> XpMode.DEFENCE
                    else -> XpMode.ATTACK
                }
            }

            player.hasWeaponType(WeaponType.SPEAR) -> {
                when (style) {
                    3 -> XpMode.DEFENCE
                    else -> XpMode.SHARED
                }
            }

            player.hasWeaponType(WeaponType.TRIDENT) -> {
                when (style) {
                    3 -> XpMode.SHARED
                    else -> XpMode.MAGIC
                }
            }

            player.hasWeaponType(WeaponType.SCYTHE) -> {
                when (style) {
                    0 -> XpMode.ATTACK
                    1 -> XpMode.STRENGTH
                    2 -> XpMode.STRENGTH
                    else -> XpMode.DEFENCE
                }
            }

            player.hasWeaponType(WeaponType.HALBERD) -> {
                when (style) {
                    0 -> XpMode.SHARED
                    1 -> XpMode.STRENGTH
                    else -> XpMode.DEFENCE
                }
            }

            player.hasWeaponType(WeaponType.STAFF_HALBERD) -> {
                when (style) {
                    0 -> XpMode.ATTACK
                    1 -> XpMode.STRENGTH
                    else -> XpMode.DEFENCE
                }
            }

            player.hasWeaponType(WeaponType.BLUDGEON) -> XpMode.STRENGTH

            player.hasWeaponType(WeaponType.BULWARK) -> XpMode.ATTACK

            player.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) -> {
                when (style) {
                    3 -> XpMode.SHARED
                    else -> XpMode.RANGED
                }
            }

            player.hasWeaponType(WeaponType.SALAMANDER) -> {
                when (style) {
                    0 -> XpMode.STRENGTH
                    1 -> XpMode.RANGED
                    else -> XpMode.MAGIC
                }
            }

            else -> XpMode.ATTACK
        }
    }
}

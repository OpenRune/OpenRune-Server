package org.rsmod.api.combat.formulas.attributes.collector

import dev.openrune.types.ItemServerType
import java.util.EnumSet
import kotlin.collections.plusAssign
import org.rsmod.api.combat.commons.magic.MagicSpellChecks
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.formulas.attributes.CombatSpellAttributes
import org.rsmod.api.combat.formulas.attributes.CombatStaffAttributes
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.getOrNull

public class CombatMagicAttributeCollector {
    // `random` is an explicit parameter to indicate that this function relies on randomness
    // for certain effects, such as the Brimstone ring proc.
    public fun spellCollect(
        player: Player,
        spell: ItemServerType,
        spellbook: Spellbook?,
        usedSunfireRune: Boolean,
        random: GameRandom,
    ): EnumSet<CombatSpellAttributes> {
        val attributes = EnumSet.noneOf(CombatSpellAttributes::class.java)

        if (spellbook == Spellbook.Standard) {
            attributes += CombatSpellAttributes.StandardBook
        }

        val spellAttribute =
            when {
                MagicSpellChecks.isWindSpell(spell) -> CombatSpellAttributes.WindSpell
                MagicSpellChecks.isWaterSpell(spell) -> CombatSpellAttributes.WaterSpell
                MagicSpellChecks.isEarthSpell(spell) -> CombatSpellAttributes.EarthSpell
                MagicSpellChecks.isFireSpell(spell) -> CombatSpellAttributes.FireSpell
                MagicSpellChecks.isBindSpell(spell) -> CombatSpellAttributes.BindSpell
                spell.isType("obj.50_magic_dart") -> CombatSpellAttributes.MagicDart
                else -> null
            }

        if (spellAttribute != null) {
            attributes += spellAttribute
        }

        val helm = player.hat
        val body = player.torso
        val legs = player.legs
        val weapon = player.righthand
        val amulet = player.front

        val hasImprovedAhrimPassive =
            EquipmentChecks.isAhrimSet(helm, body, legs, weapon) &&
                amulet.isType("obj.damned_amulet")

        if (hasImprovedAhrimPassive && random.randomBoolean(4)) {
            attributes += CombatSpellAttributes.AhrimPassive
        }

        val ring = player.ring
        if (ring.isType("obj.brimstone_ring") && random.randomBoolean(4)) {
            attributes += CombatSpellAttributes.BrimstonePassive
        }

        if (player.vars["varbit.buff_mark_of_darkness_disabled"] == 1) {
            attributes += CombatSpellAttributes.MarkOfDarkness
        }

        if (weapon.isType("obj.slayer_staff_enchanted")) {
            attributes += CombatSpellAttributes.SlayerStaffE
        }

        val gloves = player.hands
        if (gloves.isType("obj.gauntlets_of_chaos")) {
            attributes += CombatSpellAttributes.ChaosGauntlets
        }

        if (MagicSpellChecks.isBoltSpell(spell)) {
            attributes += CombatSpellAttributes.BoltSpell
        }

        val chargeBuffCooldown = player.vars["varbit.buff_charge_spell_disabled"]
        if (MagicSpellChecks.isGodSpell(spell) && chargeBuffCooldown > 0) {
            attributes += CombatSpellAttributes.ChargeSpell
        }

        if (MagicSpellChecks.isDemonbaneSpell(spell)) {
            attributes += CombatSpellAttributes.Demonbane
        }

        if (EquipmentChecks.isSmokeStaff(weapon)) {
            attributes += CombatSpellAttributes.SmokeStaff
        }

        if (player.skullIcon == constants.skullicon_forinthry_surge) {
            attributes += CombatSpellAttributes.ForinthrySurge
        }

        if (amulet.isType("obj.wild_cave_amulet")) {
            attributes += CombatSpellAttributes.AmuletOfAvarice
        } else if (amulet.isType("obj.nzone_salve_amulet_e")) {
            attributes += CombatSpellAttributes.SalveAmuletEi
        } else if (amulet.isType("obj.nzone_salve_amulet")) {
            attributes += CombatSpellAttributes.SalveAmuletI
        }

        val helmType = getOrNull(player.hat)
        if (helmType != null && helmType.hasImbuedBlackMaskAttribute()) {
            attributes += CombatSpellAttributes.BlackMaskI
        }

        val weaponAttribute =
            when {
                weapon.isType("obj.dragonhunter_wand") -> {
                    CombatSpellAttributes.DragonHunterWand
                }

                weapon.isType("obj.dragonhunter_lance") -> {
                    CombatSpellAttributes.DragonHunterLance
                }

                EquipmentChecks.isDragonHunterCrossbow(weapon) -> {
                    CombatSpellAttributes.DragonHunterCrossbow
                }

                weapon.isAnyType(
                    "obj.wild_cave_accursed_charged",
                    "obj.wild_cave_accursed_charged_recol",
                    "obj.wild_cave_sceptre_charged",
                    "obj.wild_cave_sceptre_charged_recol",
                ) -> {
                    CombatSpellAttributes.RevenantWeapon
                }

                weapon.isType("obj.purging_staff") -> {
                    CombatSpellAttributes.PurgingStaff
                }

                else -> null
            }

        if (weaponAttribute != null) {
            attributes += weaponAttribute
        }

        val shield = player.lefthand

        val shieldAttribute =
            when {
                shield.isType("obj.tome_of_water") -> CombatSpellAttributes.WaterTome
                shield.isType("obj.tome_of_earth") -> CombatSpellAttributes.EarthTome
                shield.isType("obj.tome_of_fire") -> CombatSpellAttributes.FireTome
                else -> null
            }

        if (shieldAttribute != null) {
            attributes += shieldAttribute
        }

        if (MagicSpellChecks.isFireSpell(spell) && usedSunfireRune) {
            attributes += CombatSpellAttributes.SunfireRunePassive
        }

        if (ring.isType("obj.vampyre_ring")) {
            attributes += CombatSpellAttributes.EfaritaysAid
        }

        return attributes
    }

    // `random` is an explicit parameter to indicate that this function relies on randomness
    // for certain effects, such as the Brimstone ring proc.
    public fun staffCollect(player: Player, random: GameRandom): EnumSet<CombatStaffAttributes> {
        val attributes = EnumSet.noneOf(CombatStaffAttributes::class.java)

        val ring = player.ring
        if (ring.isType("obj.brimstone_ring") && random.randomBoolean(4)) {
            attributes += CombatStaffAttributes.BrimstonePassive
        }

        if (player.skullIcon == constants.skullicon_forinthry_surge) {
            attributes += CombatStaffAttributes.ForinthrySurge
        }

        val amulet = player.front
        if (amulet.isType("obj.wild_cave_amulet")) {
            attributes += CombatStaffAttributes.AmuletOfAvarice
        } else if (amulet.isType("obj.nzone_salve_amulet_e")) {
            attributes += CombatStaffAttributes.SalveAmuletEi
        } else if (amulet.isType("obj.nzone_salve_amulet")) {
            attributes += CombatStaffAttributes.SalveAmuletI
        }

        val helmType = getOrNull(player.hat)
        if (helmType != null && helmType.hasImbuedBlackMaskAttribute()) {
            attributes += CombatStaffAttributes.BlackMaskI
        }

        val weapon = player.righthand

        val weaponAttribute =
            when {
                EquipmentChecks.isTumekensShadow(weapon) -> {
                    CombatStaffAttributes.TumekensShadow
                }

                weapon.isAnyType(
                    "obj.wild_cave_accursed_charged",
                    "obj.wild_cave_accursed_charged_recol",
                    "obj.wild_cave_sceptre_charged",
                    "obj.wild_cave_sceptre_charged_recol",
                ) -> {
                    CombatStaffAttributes.RevenantWeapon
                }

                else -> null
            }

        if (weaponAttribute != null) {
            attributes += weaponAttribute
        }

        return attributes
    }

    private fun ItemServerType.hasImbuedBlackMaskAttribute(): Boolean {
        return param(BaseParams.blackmask_imbued) != 0 || param(BaseParams.slayer_helm_imbued) != 0
    }
}

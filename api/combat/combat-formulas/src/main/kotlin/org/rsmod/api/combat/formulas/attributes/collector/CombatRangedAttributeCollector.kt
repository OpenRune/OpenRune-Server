package org.rsmod.api.combat.formulas.attributes.collector

import dev.openrune.types.ItemServerType
import java.util.EnumSet
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.formulas.attributes.CombatRangedAttributes
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.player.front
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.getOrNull

public class CombatRangedAttributeCollector {
    public fun collect(
        player: Player,
        attackType: RangedAttackType?,
        attackStyle: RangedAttackStyle?,
    ): EnumSet<CombatRangedAttributes> {
        val attributes = EnumSet.noneOf(CombatRangedAttributes::class.java)

        if (attackType == RangedAttackType.Heavy) {
            attributes += CombatRangedAttributes.Heavy
        }

        val weapon = player.righthand
        val weaponType = getOrNull(weapon)
        if (weaponType != null && weaponType.isCategoryType("category.chinchompa")) {
            val chinchompaFuse =
                when (attackStyle) {
                    RangedAttackStyle.Accurate -> CombatRangedAttributes.ShortFuse
                    RangedAttackStyle.Rapid -> CombatRangedAttributes.MediumFuse
                    RangedAttackStyle.Longrange -> CombatRangedAttributes.LongFuse
                    null -> null
                }

            if (chinchompaFuse != null) {
                attributes += chinchompaFuse
            }
        }

        if (EquipmentChecks.isCrystalBow(weapon)) {
            attributes += CombatRangedAttributes.CrystalBow
        }

        val helm = player.hat
        if (EquipmentChecks.isCrystalHelm(helm)) {
            attributes += CombatRangedAttributes.CrystalHelm
        }

        val body = player.torso
        if (EquipmentChecks.isCrystalBody(body)) {
            attributes += CombatRangedAttributes.CrystalBody
        }

        val legs = player.legs
        if (EquipmentChecks.isCrystalLegs(legs)) {
            attributes += CombatRangedAttributes.CrystalLegs
        }

        if (player.skullIcon == constants.skullicon_forinthry_surge) {
            attributes += CombatRangedAttributes.ForinthrySurge
        }

        val amulet = player.front
        if (amulet.isType("obj.wild_cave_amulet")) {
            attributes += CombatRangedAttributes.AmuletOfAvarice
        } else if (amulet.isType("obj.nzone_salve_amulet_e")) {
            attributes += CombatRangedAttributes.SalveAmuletEi
        } else if (amulet.isType("obj.nzone_salve_amulet")) {
            attributes += CombatRangedAttributes.SalveAmuletI
        }

        val helmType = getOrNull(helm)
        if (helmType != null && helmType.hasImbuedBlackMaskAttribute()) {
            attributes += CombatRangedAttributes.BlackMaskI
        }

        val weaponAttribute =
            when {
                EquipmentChecks.isTwistedBow(weapon) -> {
                    CombatRangedAttributes.TwistedBow
                }

                weapon.isAnyType("obj.wild_cave_bow_charged", "obj.wild_cave_webweaver_charged") -> {
                    CombatRangedAttributes.RevenantWeapon
                }

                EquipmentChecks.isDragonHunterCrossbow(weapon) -> {
                    CombatRangedAttributes.DragonHunterCrossbow
                }

                weapon.isType("obj.scorching_bow") -> {
                    CombatRangedAttributes.ScorchingBow
                }

                weapon.isType("obj.rat_bone_bow") -> {
                    CombatRangedAttributes.RatBoneWeapon
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

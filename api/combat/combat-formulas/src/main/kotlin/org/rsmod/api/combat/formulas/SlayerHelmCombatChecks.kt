package org.rsmod.api.combat.formulas

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getOrNull

/**
 * Slayer helmet / black mask task boosts are applied at combat time when the player is on a slayer
 * task and the target is a valid task monster — not via cache params on [obj.slayer_helm].
 */
internal object SlayerHelmCombatChecks {
    private val BLACK_MASK_KEYS: Set<String> =
        setOf("nzone_black_mask") + (1..10).map { "nzone_black_mask_$it" }.toSet()
    fun InvObj?.providesMeleeTaskBoost(): Boolean {
        val type = getOrNull(this) ?: return false
        val key = type.itemKey()
        if (type.param(BaseParams.blackmask) != 0 || type.param(BaseParams.blackmask_imbued) != 0) {
            return true
        }
        if (key?.isBlackMaskItemKey() == true || key?.isImbuedBlackMaskItemKey() == true) {
            return true
        }
        return key?.isSlayerHelmItem() == true
    }

    fun InvObj?.providesImbuedRangedMagicTaskBoost(): Boolean {
        val type = getOrNull(this) ?: return false
        val key = type.itemKey()
        if (type.param(BaseParams.blackmask_imbued) != 0 || key?.isImbuedBlackMaskItemKey() == true) {
            return true
        }
        return key?.isImbuedSlayerHelmItem() == true
    }

    private fun ItemServerType.itemKey(): String? = RSCM.getReverseMapping(RSCMType.OBJ, id)

    private fun String.isBlackMaskItemKey(): Boolean = this in BLACK_MASK_KEYS

    private fun String.isImbuedBlackMaskItemKey(): Boolean = this == "nzone_black_mask_i"

    private fun String.isSlayerHelmItem(): Boolean =
        this in SLAYER_HELM_KEYS || this in IMBUED_SLAYER_HELM_KEYS

    private fun String.isImbuedSlayerHelmItem(): Boolean = this in IMBUED_SLAYER_HELM_KEYS

    private val SLAYER_HELM_KEYS: Set<String> =
        setOf(
            "obj.slayer_helm",
            "obj.slayer_helm_araxyte",
            "obj.slayer_helm_black",
            "obj.slayer_helm_green",
            "obj.slayer_helm_hooded",
            "obj.slayer_helm_hydra",
            "obj.slayer_helm_jad",
            "obj.slayer_helm_purple",
            "obj.slayer_helm_red",
            "obj.slayer_helm_turquoise",
            "obj.slayer_helm_twisted",
            "obj.slayer_helm_verzik",
            "obj.slayer_helm_zuk",
        )

    private val IMBUED_SLAYER_HELM_KEYS: Set<String> =
        setOf(
            "obj.slayer_helm_i",
            "obj.slayer_helm_i_araxyte",
            "obj.slayer_helm_i_black",
            "obj.slayer_helm_i_green",
            "obj.slayer_helm_i_hooded",
            "obj.slayer_helm_i_hydra",
            "obj.slayer_helm_i_jad",
            "obj.slayer_helm_i_purple",
            "obj.slayer_helm_i_red",
            "obj.slayer_helm_i_turquoise",
            "obj.slayer_helm_i_twisted",
            "obj.slayer_helm_i_verzik",
            "obj.slayer_helm_i_zuk",
        )
}

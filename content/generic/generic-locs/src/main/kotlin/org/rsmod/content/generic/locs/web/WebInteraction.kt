package org.rsmod.content.generic.locs.web

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.util.WeaponCategory
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.feet
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.game.inv.isType
import org.rsmod.game.type.getOrNull

object WebInteraction {
    const val NO_BLADE_MESSAGE = "Only a sharp blade can cut through this sticky web."

    fun hasAraneaBoots(access: ProtectedAccess): Boolean = with(access) {
        player.feet.isType(ARANEA_BOOTS)
    }

    fun wade(access: ProtectedAccess) {
        with(access) {
            anim(WADE_SEQ)
            soundSynth(WADE_SOUND)
        }
    }

    fun bestSlashItem(access: ProtectedAccess): ItemServerType? = with(access) {
        val equipped = getOrNull(player.righthand)
        if (equipped != null && isEligible(equipped)) {
            return equipped
        }
        return player.inv
            .mapNotNull { getOrNull(it) }
            .filter { isEligible(it) }
            .maxByOrNull { slashBonus(it) }
    }

    fun isEligible(item: ItemServerType): Boolean {
        if (isKnife(item)) return true
        if (item.weaponCategory == WeaponCategory.Unarmed) return false
        if (item.weaponCategory == WeaponCategory.Salamander) return false
        return slashBonus(item) > 0
    }

    fun slashAnimation(item: ItemServerType): String {
        if (isKnife(item)) return KNIFE_SLASH_SEQ
        val seq = item.paramOrNull(params.attack_anim_stance2) ?: return SLASH_SEQ
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id)
    }

    fun slashBonus(item: ItemServerType): Int = item.param(params.attack_slash)

    fun isKnife(item: ItemServerType): Boolean =
        item.name.equals("knife", ignoreCase = true)

    private const val ARANEA_BOOTS = "obj.aranea_boots"
    private const val WADE_SEQ = "seq.human_aranea_boots_wade"
    private const val WADE_SOUND = "synth.raft_climb"
    private const val SLASH_SEQ = "seq.human_sword_slash"
    private const val KNIFE_SLASH_SEQ = "seq.human_knife_slash"
}

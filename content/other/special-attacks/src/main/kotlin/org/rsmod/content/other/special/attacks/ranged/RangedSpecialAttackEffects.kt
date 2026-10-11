package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess

/** Plays the equipped ranged weapon's cache-defined normal attack animation and sound. */
internal fun ProtectedAccess.playRangedWeaponFx(weapon: ItemServerType): Boolean {
    val attackAnim = weapon.paramOrNull(params.attack_anim_stance1) ?: return false
    anim(RSCM.getReverseMapping(RSCMType.SEQ, attackAnim.id))
    weapon.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
    return true
}

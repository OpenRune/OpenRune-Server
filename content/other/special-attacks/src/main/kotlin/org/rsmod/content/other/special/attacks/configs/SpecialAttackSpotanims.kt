package org.rsmod.content.other.special.attacks.configs

import org.rsmod.api.type.refs.spot.SpotanimReferences

typealias special_spots = SpecialAttackSpotanims

object SpecialAttackSpotanims : SpotanimReferences() {
    val lumber_up_red = spotAnim("dragon_smallaxe_swoosh_spotanim")
    val lumber_up_silver = spotAnim("crystal_smallaxe_swoosh_spotanim")
    val fishstabber_silver = spotAnim("sp_attackglow_crystal")
    val dragon_longsword = spotAnim("sp_attack_cleave_spotanim")
}

package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.NoxiousHalberdVirulence
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.instant.InstantSpecialAttack

/**
 * Virulence instantly cures the wielder's poison or venom, then grants the next accurate noxious
 * halberd hit a minimum equal to the condition's next damage. It grants no toxin immunity.
 */
class NoxiousHalberdSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant("obj.noxious_halberd", Virulence)
        registerInstant("obj.br_noxious_halberd", Virulence)
    }

    private data object Virulence : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            val damage = cureToxinForVirulence()
            if (damage == null) {
                mes("You can only use this special attack whilst you are poisoned.")
                return false
            }

            anim("seq.human_halberd_virulence_01")
            spotanim(
                spot = "spotanim.vfx_noxious_halberd_spec",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            NoxiousHalberdVirulence.activate(player, damage)
            return true
        }

        private fun ProtectedAccess.cureToxinForVirulence(): Int? =
            when {
                PlayerVenom.isEnvenomed(player) -> {
                    val strikes = player.vars["varp.venom_strikes"]
                    val damage = PlayerVenom.damageForStrikeIndex(strikes - 1)
                    PlayerVenom.clear(player)
                    damage
                }

                PlayerPoison.isPoisoned(player) -> {
                    val severity = player.vars["varp.poison_severity"]
                    val damage = PlayerPoison.damageForSeverity(severity)
                    PlayerPoison.clear(player)
                    damage
                }

                else -> null
            }
    }
}

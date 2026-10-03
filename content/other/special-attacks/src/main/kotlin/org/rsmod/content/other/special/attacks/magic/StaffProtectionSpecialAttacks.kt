package org.rsmod.content.other.special.attacks.magic

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType

class StaffProtectionSpecialAttacks @Inject constructor() : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for ((weapon, effect) in EFFECTS) registerInstant(weapon) {
            if (player.righthand?.id != weapon.asRSCM()) return@registerInstant false
            anim(effect.first)
            spotanim(effect.second, height = 0, slot = constants.spotanim_slot_combat)
            activate(player)
            mes("Your staff protects you from half of incoming melee damage for one minute.")
            true
        }
    }

    internal companion object {
        const val END_CLOCK = "varp.staff_protection_end_clock"
        var Player.protectionEnd by intVarp(END_CLOCK)
        val EFFECTS = mapOf(
            "obj.sotd" to ("seq.sotd_special" to "spotanim.sotd_special_start"),
            "obj.br_sotd" to ("seq.sotd_special" to "spotanim.sotd_special_start"),
            "obj.toxic_sotd" to ("seq.sotd_special_toxic_uncharged" to "spotanim.sotd_special_start"),
            "obj.toxic_sotd_charged" to ("seq.sotd_special_toxic_charged" to "spotanim.sotd_special_start"),
            "obj.staff_of_light" to ("seq.staff_of_light_special" to "spotanim.staff_of_light_special_start"),
            "obj.staff_of_balance" to ("seq.staff_of_balance_special" to "spotanim.staff_of_balance_special_start"),
            "obj.toxic_sotd_deadman" to ("seq.deadman_2026_sotd_special_toxic_uncharged" to "spotanim.deadman_2026_sotd_special_start"),
            "obj.toxic_sotd_charged_deadman" to ("seq.deadman_2026_sotd_special_toxic_charged" to "spotanim.deadman_2026_sotd_special_start"),
        )

        fun activate(player: Player) { player.protectionEnd = player.currentMapClock + 100 }
        fun clear(player: Player) { player.protectionEnd = 0 }

        fun modify(player: Player, hit: Hit): Hit {
            if (player.protectionEnd == 0) return hit
            if (player.currentMapClock >= player.protectionEnd) { clear(player); return hit }
            if (hit.damage <= 0) return hit
            if (EFFECTS.keys.none { it.asRSCM() == player.righthand?.id }) {
                clear(player)
                return hit
            }
            return if (hit.type == HitType.Melee) hit.copy(hitmark = hit.hitmark.copy(damage = hit.damage / 2)) else hit
        }
    }
}

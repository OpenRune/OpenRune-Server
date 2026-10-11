package org.rsmod.api.player.hit.modifier

import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.hit.PlayerAbsorption
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder

/**
 * Same as [StandardPlayerHitModifier], but skips the protection-prayer damage reduction step -
 * for specials that are documented to ignore Protect from Melee/Missiles/Magic (Ancient mace,
 * Dragon sword's Wild Stab). God mode and NPC absorption still apply.
 */
public object BypassProtectionPrayerPlayerHitModifier : PlayerHitModifier {
    override fun HitBuilder.modify(target: Player) {
        if (target.adminGodMode) {
            damage = 0
            return
        }

        if (isFromNpc) {
            damage = PlayerAbsorption.absorb(player = target, incomingDamage = damage)
        }
    }
}

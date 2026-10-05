package org.rsmod.api.player.hit.modifier

import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder

public object NoopPlayerHitModifier : PlayerHitModifier {
    override fun HitBuilder.modify(target: Player) {
        if (target.adminGodMode) {
            damage = 0
        }
    }
}

package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerInvUpdateHook
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

class WildernessLootKeySkullHook @Inject constructor() : PlayerInvUpdateHook {
    override fun onInvUpdated(player: Player, inv: Inventory) {
        if (inv !== player.inv) {
            return
        }
        player.refreshSkullIconIfLootKeysChanged()
    }
}

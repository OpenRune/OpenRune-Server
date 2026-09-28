package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

internal class BarrowsPresenceHook @Inject constructor(private val spawner: BarrowsNpcSpawner) :
    PlayerPostTickHook {
    override fun onPostTick(player: Player) {
        if (!player.inBarrowsCrypt()) return
        player.refreshLadder()
        for (npc in spawner.owned(player).toList()) {
            if (npc.hitpoints <= 0) continue
            val near =
                npc.coords.level == player.coords.level &&
                    npc.coords.chebyshevDistance(player.coords) < LEASH_DISTANCE
            if (near) continue
            if (npc.isBrother()) player.mes("We'll finish this later...")
            spawner.despawn(player, npc)
        }
    }

    private companion object {
        const val LEASH_DISTANCE = 15
    }
}

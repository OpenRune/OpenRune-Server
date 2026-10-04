package org.rsmod.api.bosses.runtime

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.mechanics.toxins.impl.PlayerBleed
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

@Singleton
class BossBleeds @Inject constructor(private val playerList: PlayerList) {
    fun apply(
        owner: Npc,
        player: Player,
        duration: Int,
        stillInterval: Int = 0,
        onApply: (Player) -> Unit = {},
        onStill: (Player) -> Unit = {},
        onMoving: (Player) -> Unit,
    ) {
        PlayerBleed.apply(owner, player, duration, stillInterval, onApply, onStill, onMoving)
    }

    fun isBleeding(player: Player): Boolean = PlayerBleed.isBleeding(player)

    fun remove(owner: Npc, player: Player): Boolean {
        if (PlayerBleed.ownerOf(player) !== owner) return false
        PlayerBleed.clear(player)
        return true
    }

    fun clear(owner: Npc) {
        for (player in playerList) {
            if (PlayerBleed.ownerOf(player) === owner) PlayerBleed.clear(player)
        }
    }
}

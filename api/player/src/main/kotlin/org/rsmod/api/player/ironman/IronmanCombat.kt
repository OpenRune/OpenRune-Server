package org.rsmod.api.player.ironman

import org.rsmod.game.damage.DamageContributor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public fun Npc.isDamagedByOtherPlayer(player: Player): Boolean {
    val uuid = player.uuid ?: return false
    return damageContributions.entries().any {
        it is DamageContributor.ByPlayer && it.uuid != uuid
    }
}

public fun Player.shouldBlockNpcCombatXp(npc: Npc): Boolean =
    isSoloIronman && npc.isDamagedByOtherPlayer(this)

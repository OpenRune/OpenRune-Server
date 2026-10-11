package org.rsmod.api.player.ironman

import org.rsmod.api.config.refs.params
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
    // Revenants allow shared combat; native hero selection still awards loot only to the MVP.
    isSoloIronman && npc.type.param(params.revenant) == 0 && npc.isDamagedByOtherPlayer(this)

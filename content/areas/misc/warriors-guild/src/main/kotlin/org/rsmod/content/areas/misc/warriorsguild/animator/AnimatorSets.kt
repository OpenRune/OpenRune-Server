package org.rsmod.content.areas.misc.warriorsguild.animator

import org.rsmod.api.invtx.invAddAll
import org.rsmod.api.invtx.invDel
import org.rsmod.api.table.WarguildAnimatorSetsRow
import org.rsmod.content.areas.misc.warriorsguild.lostArmour
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/**
 * From the moment a set is placed on the animator until its armour is killed, Shanomi is "holding"
 * it in [lostArmour]. A kill drops the set through the drop table and clears the hold; anything else
 * (despawn, logout, death) leaves the hold for Shanomi to hand back.
 */
internal object AnimatorSets {
    fun all(): List<WarguildAnimatorSetsRow> = WarguildAnimatorSetsRow.all()

    fun byIndex(index: Int): WarguildAnimatorSetsRow? = all().firstOrNull { it.index == index }

    fun byNpc(npc: Int): WarguildAnimatorSetsRow? = all().firstOrNull { it.npc.id == npc }

    fun carried(player: Player): WarguildAnimatorSetsRow? =
        all().firstOrNull { set ->
            set.helm in player.inv && set.body in player.inv && set.legs in player.inv
        }

    fun isPiece(obj: Int): Boolean =
        all().any { obj == it.helm.id || obj == it.body.id || obj == it.legs.id }

    fun take(player: Player): WarguildAnimatorSetsRow? {
        if (player.lostArmour != 0) return null
        val set = carried(player) ?: return null
        val tx = player.invDel(player.inv, set.helm, 1, set.body, 1, set.legs, 1)
        if (tx.failure) return null
        player.lostArmour = set.index
        return set
    }

    fun returnHeld(player: Player): Return {
        val set = byIndex(player.lostArmour) ?: return Return.NothingHeld
        val pieces = listOf(set.helm, set.body, set.legs).map { InvObj(it, 1) }
        val tx = player.invAddAll(player.inv, pieces)
        if (tx.failure) return Return.NoSpace
        player.lostArmour = 0
        return Return.Returned
    }

    fun releaseHold(player: Player, npc: Int) {
        val set = byNpc(npc) ?: return
        if (player.lostArmour == set.index) player.lostArmour = 0
    }

    enum class Return {
        NothingHeld,
        NoSpace,
        Returned,
    }
}

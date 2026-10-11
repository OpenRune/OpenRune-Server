package org.rsmod.content.other.containers

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.skills.magic.spell.teleports.TeleportScroll
import org.rsmod.game.entity.Player

internal object MasterScrollBook {
    const val EMPTY = "obj.bookofscrolls_empty"
    const val FILLED = "obj.bookofscrolls_charged"
    const val CAPACITY = 1000
    val forms = listOf(EMPTY, FILLED)
    fun owns(player: Player) = forms.any { it in player.inv }
    fun suffix(scroll: TeleportScroll) = scroll.item.removePrefix("obj.teleportscroll_")
    fun count(player: Player, scroll: TeleportScroll) = if (scroll == TeleportScroll.Watson)
        player.vars["varbit.bookofscrolls_watson_highbits"] * 256 + player.vars["varbit.bookofscrolls_watson_lowbits"]
        else player.vars["varbit.bookofscrolls_${suffix(scroll)}"]
    fun setCount(player: Player, scroll: TeleportScroll, count: Int) {
        require(count in 0..CAPACITY)
        if (scroll == TeleportScroll.Watson) {
            VarPlayerIntMapSetter.set(player, "varbit.bookofscrolls_watson_highbits", count / 256)
            VarPlayerIntMapSetter.set(player, "varbit.bookofscrolls_watson_lowbits", count % 256)
        } else VarPlayerIntMapSetter.set(player, "varbit.bookofscrolls_${suffix(scroll)}", count)
    }

    // Native proc 5157 multiplies high bits by 15; this is not a bitshift.
    fun defaultId(player: Player) = player.vars["varbit.bookofscrolls_selected_teleport_highbits"] * 15 + player.vars["varbit.bookofscrolls_selected_teleport_lowbits"]
    fun setDefault(player: Player, id: Int) {
        require(id in 0..TeleportScroll.entries.size)
        val high = (id - 1).coerceAtLeast(0) / 15
        VarPlayerIntMapSetter.set(player, "varbit.bookofscrolls_selected_teleport_highbits", high)
        VarPlayerIntMapSetter.set(player, "varbit.bookofscrolls_selected_teleport_lowbits", id - high * 15)
    }
    fun total(player: Player) = TeleportScroll.entries.sumOf { count(player, it) }
}

package org.rsmod.content.interfaces.bank.util

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

object BankTransforms {
    fun transform(player: Player, bank: Inventory, slot: Int, result: ItemServerType): Boolean {
        val source = bank[slot] ?: return false
        if (source.id == result.id) return false
        val target = bank.indexOfFirst {
            it?.id == result.id || result.placeholderLink > 0 && it?.id == result.placeholderLink
        }
        if (target >= 0) {
            val existing = checkNotNull(bank[target])
            val count = source.count.toLong() + if (existing.id == result.id) existing.count else 0
            if (count > Int.MAX_VALUE) return false
            bank[target] = InvObj(result, count.toInt())
            removeSlot(player, bank, slot)
        } else {
            removeSlot(player, bank, slot)
            val last = bank.objs.indexOfLast { it != null }
            bank[last + 1] = InvObj(result, source.count)
        }
        val customSlots = BankTab.tabs.sumOf { player.vars[it.sizeVarBit] }
        val mainSlots = bank.objs.indexOfLast { it != null } + 1 - customSlots
        VarPlayerIntMapSetter.set(player, BankTab.Main.sizeVarBit, mainSlots)
        return true
    }

    private fun removeSlot(player: Player, bank: Inventory, slot: Int) {
        var end = 0
        for (tab in BankTab.tabs) {
            val size = player.vars[tab.sizeVarBit]
            end += size
            if (slot >= end) continue
            VarPlayerIntMapSetter.set(player, tab.sizeVarBit, size - 1)
            if (size == 1) {
                for (next in tab.index until BankTab.tabs.lastIndex) {
                    VarPlayerIntMapSetter.set(
                        player, BankTab.tabs[next].sizeVarBit,
                        player.vars[BankTab.tabs[next + 1].sizeVarBit],
                    )
                }
                VarPlayerIntMapSetter.set(player, BankTab.tabs.last().sizeVarBit, 0)
                val selected = player.vars["varbit.bank_currenttab"]
                if (selected >= tab.varValue) {
                    VarPlayerIntMapSetter.set(
                        player, "varbit.bank_currenttab",
                        if (selected == tab.varValue) 0 else selected - 1,
                    )
                }
            }
            break
        }
        val last = bank.objs.indexOfLast { it != null }
        for (next in slot until last) bank[next] = bank[next + 1]
        bank[last] = null
    }
}

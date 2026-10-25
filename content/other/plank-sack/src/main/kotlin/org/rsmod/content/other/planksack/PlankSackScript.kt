package org.rsmod.content.other.planksack

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PlankSackScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(PlankSack.SACK) { fill(PlankSack.planks) }
        onOpHeld3(PlankSack.SACK) { check() }
        onOpHeld4(PlankSack.SACK) { empty() }
        for (plank in PlankSack.planks) {
            onOpHeldU(plank.obj, PlankSack.SACK) { fill(listOf(plank)) }
        }
    }

    fun ProtectedAccess.fill(types: List<PlankSack.Plank>): Int {
        val held = types.filter { inv.physicalCount(it.obj) > 0 }
        if (held.isEmpty()) {
            mes(NO_PLANKS)
            return 0
        }
        var filled = 0
        for (plank in held) {
            val amount = minOf(inv.physicalCount(plank.obj), PlankSack.freeSpace(player))
            if (amount <= 0) break
            if (invDel(inv, plank.obj, amount, ignoreVirtualStorage = true).failure) continue
            filled += PlankSack.add(player, plank, amount)
        }
        if (filled == 0) mes(FULL)
        return filled
    }

    fun ProtectedAccess.empty(): Int {
        if (PlankSack.total(player) == 0) {
            mes(EMPTY)
            return 0
        }
        var emptied = 0
        for (plank in PlankSack.planks) {
            val amount = minOf(PlankSack.stored(player, plank), inv.freeSpace())
            if (amount <= 0) continue
            if (invAdd(inv, plank.obj, amount, ignoreVirtualStorage = true).failure) continue
            emptied += PlankSack.remove(player, plank, amount)
        }
        if (PlankSack.total(player) > 0) mes(NO_SPACE)
        return emptied
    }

    private fun ProtectedAccess.check() {
        if (PlankSack.total(player) == 0) {
            mes(EMPTY)
            return
        }
        for (plank in PlankSack.planks) {
            val stored = PlankSack.stored(player, plank)
            if (stored > 0) mes("${plank.label}: $stored")
        }
    }

    private companion object {
        const val EMPTY = "Your sack is currently empty."
        const val NO_PLANKS = "You don't have any planks to put in the sack."
        const val FULL = "Your sack is full."
        const val NO_SPACE = "You don't have enough inventory space to empty the sack."
    }
}

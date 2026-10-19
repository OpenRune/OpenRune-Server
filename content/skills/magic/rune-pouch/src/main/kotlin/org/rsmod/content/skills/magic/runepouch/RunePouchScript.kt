package org.rsmod.content.skills.magic.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfModalDrag
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RunePouchScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (pouch in RunePouch.pouches) {
            onOpHeld1(pouch) { openPouch() }
            onOpHeld4(pouch) { emptyPouch() }
            for (rune in RunePouch.runes) {
                onOpHeldU(rune, pouch) { storeAll(rune) }
            }
        }
        onIfModalButton(POUCH) { pouchOp(it.comsub, it.op) }
        onIfModalButton(INVENTORY) { inventoryOp(it.comsub, it.op) }
        onIfModalDrag(POUCH) { drag ->
            val from = drag.selectedSlot ?: return@onIfModalDrag
            val into = drag.targetSlot ?: return@onIfModalDrag
            val slots = RunePouch.slotCount(player)
            if (from in 0 until slots && into in 0 until slots) RunePouch.swap(player, from, into)
        }
        onIfModalDrag(INVENTORY) { drag ->
            val from = drag.selectedSlot ?: return@onIfModalDrag
            val into = drag.targetSlot ?: return@onIfModalDrag
            invMoveToSlot(inv, inv, from, into)
        }
        onIfModalButton("component.rune_pouch:1") { selectQuantity(Quantity.One) }
        onIfModalButton("component.rune_pouch:5") { selectQuantity(Quantity.Five) }
        onIfModalButton("component.rune_pouch:all") { selectQuantity(Quantity.All) }
        onIfModalButton("component.rune_pouch:x") {
            if (it.op == IfButtonOp.Op2) {
                val amount = countDialog()
                if (amount > 0) vars[CUSTOM_QUANTITY] = amount
                selectQuantity(Quantity.Custom)
            } else if (vars[CUSTOM_QUANTITY] > 0) {
                selectQuantity(Quantity.Custom)
            }
        }
    }

    private fun ProtectedAccess.openPouch() {
        vars["varp.if1"] = RunePouch.slotCount(player)
        ifOpenMainModal("interface.rune_pouch")
        ifSetEvents(POUCH, 0..5, *SLOT_EVENTS)
        ifSetEvents(INVENTORY, inv.indices, *SLOT_EVENTS)
    }

    private fun ProtectedAccess.selectQuantity(quantity: Quantity) {
        vars[SELECTED_QUANTITY] = quantity.varValue
    }

    private suspend fun ProtectedAccess.pouchOp(comsub: Int, op: IfButtonOp) {
        if (comsub !in 0 until RunePouch.slotCount(player)) return
        val (rune, _) = RunePouch.slot(player, comsub) ?: return
        if (op == IfButtonOp.Op10) {
            examine(rune)
            return
        }
        val amount = requestedAmount(op) ?: return
        withdraw(comsub, amount)
    }

    private suspend fun ProtectedAccess.inventoryOp(slot: Int, op: IfButtonOp) {
        val obj = inv[slot] ?: return
        val internal = RSCM.getReverseMapping(RSCMType.OBJ, obj.id)
        if (op == IfButtonOp.Op10) {
            objExamine(inv, slot)
            return
        }
        if (!RunePouch.isRune(internal)) {
            mes(NOT_A_RUNE)
            return
        }
        val amount = requestedAmount(op) ?: return
        store(internal, amount)
    }

    private suspend fun ProtectedAccess.requestedAmount(op: IfButtonOp): Int? {
        val choice =
            when (op) {
                IfButtonOp.Op1 ->
                    Quantity.entries.firstOrNull { it.varValue == vars[SELECTED_QUANTITY] }
                        ?: Quantity.One
                IfButtonOp.Op2 -> Quantity.One
                IfButtonOp.Op3 -> Quantity.Five
                IfButtonOp.Op4 -> Quantity.Custom
                IfButtonOp.Op5 -> Quantity.X
                IfButtonOp.Op6 -> Quantity.All
                else -> return null
            }
        val custom = vars[CUSTOM_QUANTITY]
        return when (choice) {
            Quantity.One -> 1
            Quantity.Five -> 5
            Quantity.All -> Int.MAX_VALUE
            Quantity.Custom -> if (custom > 0) custom else countDialog().takeIf { it > 0 }
            Quantity.X -> countDialog().takeIf { it > 0 }
        }
    }

    fun ProtectedAccess.store(rune: String, requested: Int): Int {
        val slots = RunePouch.slotCount(player)
        if (slots == 0) return 0
        val held = inv.physicalCount(rune)
        if (held <= 0) return 0
        val target = RunePouch.slotFor(player, rune, slots)
        if (target == null) {
            mes(NO_FREE_SLOT)
            return 0
        }
        val space = RunePouch.CAPACITY - (RunePouch.slot(player, target)?.second ?: 0)
        if (space <= 0) {
            mes(FULL)
            return 0
        }
        val amount = minOf(requested, held, space)
        if (invDel(inv, rune, amount, ignoreVirtualStorage = true).failure) return 0
        return RunePouch.add(player, rune, amount, slots)
    }

    fun ProtectedAccess.withdraw(slot: Int, requested: Int): Int {
        val (rune, stored) = RunePouch.slot(player, slot) ?: return 0
        val amount = minOf(requested, stored)
        if (invAdd(inv, rune, amount, ignoreVirtualStorage = true).failure) {
            mes(NO_SPACE)
            return 0
        }
        return RunePouch.remove(player, slot, amount)
    }

    private fun ProtectedAccess.storeAll(rune: String) {
        store(rune, Int.MAX_VALUE)
    }

    fun ProtectedAccess.emptyPouch() {
        val slots = RunePouch.slotCount(player)
        if ((0 until slots).none { RunePouch.slot(player, it) != null }) {
            mes(EMPTY)
            return
        }
        for (slot in 0 until slots) {
            if (RunePouch.slot(player, slot) == null) continue
            if (withdraw(slot, Int.MAX_VALUE) == 0) return
        }
    }

    private fun ProtectedAccess.examine(rune: String) {
        val type = ServerCacheManager.getItem(rune.asRSCM(RSCMType.OBJ)) ?: return
        player.objExamine(type, 1, 0)
    }

    private enum class Quantity(val varValue: Int) {
        One(0),
        Five(1),
        All(2),
        Custom(3),
        X(-1),
    }

    private companion object {
        const val POUCH = "component.rune_pouch:pouch"
        const val INVENTORY = "component.rune_pouch:inventory"
        const val SELECTED_QUANTITY = "varbit.rune_pouch_selectedquantity"
        const val CUSTOM_QUANTITY = "varbit.rune_pouch_customquantity"

        const val NOT_A_RUNE = "You can only store runes in the rune pouch."
        const val NO_FREE_SLOT = "Your rune pouch has no room for another type of rune."
        const val FULL = "Your rune pouch cannot hold any more of those runes."
        const val NO_SPACE = "You don't have enough inventory space."
        const val EMPTY = "Your rune pouch is empty."

        val SLOT_EVENTS =
            arrayOf(
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op6,
                IfEvent.Op10,
                IfEvent.Depth1,
                IfEvent.DragTarget,
            )
    }
}

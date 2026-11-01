package org.rsmod.content.skills.hunter

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.enums.enum
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HuntsmansKitScript : PluginScript() {
    private val storable: Set<String> by lazy {
        enum<Int, ItemServerType>("huntsmans_kit_storable").values.filterNotNull().mapTo(HashSet()) { it.internalName }
    }

    private val Player.kit: Inventory
        get() = invMap.getOrPut(INV)

    private var ProtectedAccess.quantity: Int by intVarBit("varbit.ii_elnock_storage_selectedquantity")

    override fun ScriptContext.startup() {
        onOpHeld1(KIT) { view() }
        onOpHeld2(KIT) { fill() }
        onOpHeld3(KIT) { empty() }
        onOpHeldU(KIT) { store(it.second) }
        onIfOpen(MAIN) { player.startInvTransmit(player.kit) }
        onIfClose(MAIN) { player.stopInvTransmit(player.kit) }
        onIfModalButton("component.huntsmans_kit:items") { withdrawButton(it) }
        onIfModalButton("component.huntsmans_kit_side:items") { depositButton(it) }
        onIfModalButton("component.huntsmans_kit:fill") { fill() }
        onIfModalButton("component.huntsmans_kit:empty") { empty() }
        onIfModalButton("component.huntsmans_kit:1") { quantity = 0 }
        onIfModalButton("component.huntsmans_kit:5") { quantity = 1 }
        onIfModalButton("component.huntsmans_kit:all") { quantity = 2 }
        onIfModalButton("component.huntsmans_kit:x") { quantity = 3 }
        onEvent<HeldDropEvents.Destroy> { if (obj.id == KIT.asRSCM(RSCMType.OBJ)) player.kit.fillNulls() }
    }

    private fun ProtectedAccess.view() {
        player.startInvTransmit(player.kit)
        ifOpenMainSidePair(MAIN, SIDE)
        ifSetEvents("component.huntsmans_kit:items", 0 until player.kit.size, *ITEM_EVENTS)
        ifSetEvents("component.huntsmans_kit_side:items", inv.indices, *ITEM_EVENTS)
    }

    private fun ProtectedAccess.store(type: ItemServerType) {
        if (type.internalName == KIT) {
            mes("The Huntsman's Kit cannot hold itself! How weird would that be?")
            return
        }
        deposit(type.internalName, Int.MAX_VALUE)
    }

    private fun ProtectedAccess.fill() {
        val held = inv.mapNotNullTo(LinkedHashSet()) { obj -> obj?.let { RSCM.getReverseMapping(RSCMType.OBJ, it.id) } }
        val kept = held.filter { it in storable }
        if (kept.isEmpty()) {
            mes("You have nothing that can be stored in the huntsman's kit.")
            return
        }
        if (kept.sumOf { move(inv, player.kit, it, Int.MAX_VALUE) } == 0) mes("Your huntsman's kit is full.")
    }

    private fun ProtectedAccess.empty() {
        val kit = player.kit
        if (kit.isEmpty()) {
            mes("Your huntsman's kit is empty.")
            return
        }
        for (slot in kit.indices.reversed()) {
            val obj = kit[slot] ?: continue
            move(kit, inv, RSCM.getReverseMapping(RSCMType.OBJ, obj.id), obj.count)
        }
    }

    private suspend fun ProtectedAccess.withdrawButton(event: IfModalButton) {
        val obj = player.kit[event.comsub] ?: return
        if (event.op == IfButtonOp.Op10) {
            objExamine(player.kit, event.comsub)
            return
        }
        val name = RSCM.getReverseMapping(RSCMType.OBJ, obj.id)
        move(player.kit, inv, name, count(event.op))
    }

    private suspend fun ProtectedAccess.depositButton(event: IfModalButton) {
        val obj = inv[event.comsub] ?: return
        if (event.op == IfButtonOp.Op10) {
            objExamine(inv, event.comsub)
            return
        }
        deposit(RSCM.getReverseMapping(RSCMType.OBJ, obj.id), count(event.op))
    }

    private fun ProtectedAccess.deposit(obj: String, count: Int): Int {
        if (obj !in storable) {
            mes("You can't store that in the huntsman's kit.")
            return 0
        }
        val moved = move(inv, player.kit, obj, count)
        if (moved == 0) mes("Your huntsman's kit is full.")
        return moved
    }

    private fun ProtectedAccess.move(from: Inventory, into: Inventory, obj: String, count: Int): Int {
        val id = obj.asRSCM(RSCMType.OBJ)
        val before = invTotal(from, obj)
        for (slot in from.indices.reversed()) {
            val moved = before - invTotal(from, obj)
            if (moved >= count) break
            val held = from[slot] ?: continue
            if (held.id != id) continue
            invMoveFromSlot(from = from, into = into, fromSlot = slot, count = minOf(count - moved, held.count), strict = false)
        }
        return before - invTotal(from, obj)
    }

    private suspend fun ProtectedAccess.count(op: IfButtonOp): Int =
        when (op) {
            IfButtonOp.Op1 ->
                when (quantity) {
                    1 -> 5
                    2 -> Int.MAX_VALUE
                    3 -> countDialog()
                    else -> 1
                }
            IfButtonOp.Op3 -> 5
            IfButtonOp.Op4 -> countDialog()
            IfButtonOp.Op5 -> Int.MAX_VALUE
            else -> 1
        }

    private companion object {
        const val KIT = "obj.huntsmans_kit"
        const val INV = "inv.huntsmans_kit"
        const val MAIN = "interface.huntsmans_kit"
        const val SIDE = "interface.huntsmans_kit_side"
        val ITEM_EVENTS =
            arrayOf(
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op10,
                IfEvent.Depth1,
                IfEvent.DragTarget,
            )
    }
}

package org.rsmod.content.skills.fishing.scripts

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.player.output.UpdateInventory
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.table.fishing.FishingMethodRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.objtx.isOk
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TackleBoxScript @Inject constructor() : PluginScript() {

    private val storable: Set<String> by lazy {
        val configured = enum<Int, ItemServerType>("tackle_box_storable")
        FishingMethodRow.all()
            .flatMap { listOfNotNull(it.tool, it.bait, it.altTool) }
            .toSet() + configured.values.filterNotNull().map { it.internalName }
    }

    private val Player.tackleBox: Inventory
        get() = invMap.getOrPut("inv.tackle_box")

    private var ProtectedAccess.selectedQtyId: Int by intVarBit("varbit.ii_elnock_storage_selectedquantity")

    private fun comp(name: String): Int = name.asRSCM(RSCMType.COMPONENT)

    override fun ScriptContext.startup() {
        onOpHeld1(BOX) { openBox() }
        onOpHeld2(BOX) { depositFromBank() }
        onOpHeldU(BOX) { depositHeld(it.second, it.secondSlot) }

        onIfOpen("interface.tackle_box_main") { player.startInvTransmit(player.tackleBox) }
        onIfClose("interface.tackle_box_main") { player.stopInvTransmit(player.tackleBox) }

        onIfModalButton("component.tackle_box_main:items") { handleWithdraw(it) }
        onIfModalButton("component.tackle_box_side:items") { handleDeposit(it) }
        onIfModalButton("component.tackle_box_main:fill") { fillBox() }
        onIfModalButton("component.tackle_box_main:empty") { emptyBox() }

        onIfModalButton("component.tackle_box_main:1") { selectedQtyId = 0 }
        onIfModalButton("component.tackle_box_main:5") { selectedQtyId = 1 }
        onIfModalButton("component.tackle_box_main:x") { selectedQtyId = 3 }
        onIfModalButton("component.tackle_box_main:all") { selectedQtyId = 2 }
    }

    private fun ProtectedAccess.openBox() {
        player.startInvTransmit(player.tackleBox)
        ifOpenMainSidePair("interface.tackle_box_main", "interface.tackle_box_side")
        runClientScript(
            4049,
            comp("component.tackle_box_main:universe"),
            comp("component.tackle_box_main:fill"),
            comp("component.tackle_box_main:empty"),
            comp("component.tackle_box_main:items"),
            comp("component.tackle_box_main:scrollbar"),
            comp("component.tackle_box_main:1"),
            comp("component.tackle_box_main:5"),
            comp("component.tackle_box_main:x"),
            comp("component.tackle_box_main:all"),
        )
        runClientScript(4061, comp("component.tackle_box_side:items"))
        ifSetEvents(
            "component.tackle_box_main:items",
            player.tackleBox.indices,
            IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
            IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op10,
        )
        ifSetEvents(
            "component.tackle_box_side:items",
            0..27,
            IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
            IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op10,
        )
        ifSetEvents("component.tackle_box_main:fill", 0..0, IfEvent.Op1)
        ifSetEvents("component.tackle_box_main:empty", 0..0, IfEvent.Op1)
        ifSetEvents("component.tackle_box_main:1", 0..0, IfEvent.Op1)
        ifSetEvents("component.tackle_box_main:5", 0..0, IfEvent.Op1)
        ifSetEvents("component.tackle_box_main:x", 0..0, IfEvent.Op1)
        ifSetEvents("component.tackle_box_main:all", 0..0, IfEvent.Op1)
    }

    private suspend fun ProtectedAccess.handleWithdraw(event: IfModalButton) {
        val box = player.tackleBox
        val clicked = box[event.comsub] ?: return
        if (event.op == IfButtonOp.Op10) {
            objExamine(box, event.comsub)
            return
        }
        val available = invTotal(box, getInvObj(clicked).internalName)
        var remaining = min(resolveCount(event.op, available), available)
        for (s in box.indices) {
            if (remaining <= 0) break
            val o = box[s] ?: continue
            if (o.id != clicked.id) continue
            val take = min(remaining, o.count)
            val result = invMoveFromSlot(from = box, into = inv, fromSlot = s, count = take, strict = false)
            if (!result[0].isOk()) {
                mes("Your inventory is too full.")
                break
            }
            remaining -= take
        }
        UpdateInventory.updateInvFull(player, box)
    }

    /**
     * The "Use" option while the bank is open sweeps every unnoted supported item out of the
     * inventory and into the box.
     */
    private fun ProtectedAccess.depositFromBank() {
        if (BANK_INTERFACE !in player.ui) {
            mes("You can only do that while your bank is open.")
            return
        }
        val box = player.tackleBox
        var moved = 0
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            val type = getInvObj(obj)
            if (type.isCert || type.internalName !in storable || !box.hasRoomFor(obj.id)) {
                continue
            }
            val result = invMoveFromSlot(from = inv, into = box, fromSlot = slot, count = obj.count, strict = false)
            if (result[0].isOk()) {
                moved += obj.count
            }
        }
        mes(if (moved > 0) "You store your fishing equipment in the tackle box." else "You have no fishing equipment to store.")
        UpdateInventory.updateInvFull(player, box)
    }

    private fun ProtectedAccess.depositHeld(type: ItemServerType, slot: Int) {
        if (type.internalName !in storable) {
            mes("You can only store fishing equipment in the tackle box.")
            return
        }
        val obj = inv[slot] ?: return
        if (!player.tackleBox.hasRoomFor(obj.id)) {
            mesTackleBoxFull()
            return
        }
        val result =
            invMoveFromSlot(from = inv, into = player.tackleBox, fromSlot = slot, count = obj.count, strict = false)
        if (result[0].isOk()) {
            mes("You store your ${type.name.lowercase()} in the tackle box.")
        } else {
            mes("The tackle box is full.")
        }
    }

    private suspend fun ProtectedAccess.handleDeposit(event: IfModalButton) {
        val clicked = inv[event.comsub] ?: return
        if (event.op == IfButtonOp.Op10) {
            objExamine(inv, event.comsub)
            return
        }
        val name = getInvObj(clicked).internalName
        if (name !in storable) {
            mes("You can only store fishing equipment in the tackle box.")
            return
        }
        val box = player.tackleBox
        if (!box.hasRoomFor(clicked.id)) {
            mesTackleBoxFull()
            return
        }
        val available = invTotal(inv, name)
        var remaining = min(resolveCount(event.op, available), available)
        for (s in inv.indices) {
            if (remaining <= 0) break
            val o = inv[s] ?: continue
            if (o.id != clicked.id) continue
            val take = min(remaining, o.count)
            val result = invMoveFromSlot(from = inv, into = box, fromSlot = s, count = take, strict = false)
            if (!result[0].isOk()) {
                mes("The tackle box is full.")
                break
            }
            remaining -= take
        }
        UpdateInventory.updateInvFull(player, box)
    }

    private suspend fun ProtectedAccess.resolveCount(op: IfButtonOp, available: Int): Int =
        when (op) {
            IfButtonOp.Op1 ->
                when (selectedQtyId) {
                    1 -> 5
                    2 -> available
                    3 -> countDialog()
                    else -> 1
                }
            IfButtonOp.Op2 -> 1
            IfButtonOp.Op3 -> 5
            IfButtonOp.Op4 -> countDialog()
            IfButtonOp.Op5 -> available
            else -> 1
        }

    private fun ProtectedAccess.fillBox() {
        val box = player.tackleBox
        var moved = 0
        for (name in storable) {
            val count = invTotal(inv, name)
            if (count == 0 || !box.hasRoomFor(name.asRSCM(RSCMType.OBJ))) {
                continue
            }
            invDel(inv, name, count)
            invAdd(box, name, count)
            moved += count
        }
        mes(if (moved > 0) "You fill the tackle box with your fishing equipment." else "You have no fishing equipment to store.")
        UpdateInventory.updateInvFull(player, box)
    }

    private fun Inventory.hasRoomFor(objId: Int): Boolean {
        val distinct = indices.mapNotNullTo(HashSet()) { this[it]?.id }
        return objId in distinct || distinct.size < MAX_DISTINCT_ITEMS
    }

    private fun ProtectedAccess.mesTackleBoxFull() {
        mes("The tackle box can only hold ${MAX_DISTINCT_ITEMS} different kinds of item.")
    }

    private fun ProtectedAccess.emptyBox() {
        var emptied = false
        val box = player.tackleBox
        for (name in storable) {
            val count = invTotal(box, name)
            if (count > 0) {
                invDel(box, name, count)
                invAdd(inv, name, count)
                emptied = true
            }
        }
        mes(if (emptied) "You empty the tackle box." else "The tackle box is already empty.")
        UpdateInventory.updateInvFull(player, box)
    }

    private companion object {
        private const val BOX = "obj.tackle_box"
        private const val BANK_INTERFACE = "interface.bankmain"

        /** The box only holds this many different kinds of item, no matter how many of each. */
        private const val MAX_DISTINCT_ITEMS = 35
    }
}

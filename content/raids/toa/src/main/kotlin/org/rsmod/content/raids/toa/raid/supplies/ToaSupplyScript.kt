package org.rsmod.content.raids.toa.raid.supplies

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import kotlin.math.min
import org.rsmod.api.player.output.UpdateInventory
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.IfModalDrag
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfModalDrag
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.isOk
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Helpful Spirit ([ToaSupplies]) and the supplies bag it hands out (OSRS Wiki: Supplies
 * (Tombs of Amascut)).
 *
 * - Claim opens interface 777 (`toa_midraid_loot`). Its onload script draws the three bundles
 *   from invs 807-809, so those are sent first.
 * - Choosing a bundle puts its items in the bag's inv (810, `toa_midraidloot_bag`) and a bag in
 *   the inventory, or adds to the bag already held.
 * - The bag: Open (interface 778 over the inventory), Withdraw 1 (the first item), Withdraw All
 *   (until the inventory is full), Resupply (tops up partly used supplies in the inventory from
 *   the bag, taking nothing new out). Destroy is the engine's own.
 * - In 778, each item has Withdraw-1, -5, -All (of that kind, from the clicked slot on), Drop and
 *   Examine, and can be dragged to another slot.
 * - A supply used on the bag goes into its first empty slot.
 * - When the last item leaves, "The bag disintegrates."
 *
 * The messages the wiki doesn't quote are Offline_Scape's.
 */
class ToaSupplyScript : PluginScript() {
    private val Player.bag: Inventory
        get() = invMap.getOrPut(BAG_INV)

    override fun ScriptContext.startup() {
        onOpNpc1(SPIRIT) { claim() }
        for (index in BUNDLE_INVS.indices) {
            onIfModalButton("component.toa_midraid_loot:select_button_${index + 1}") {
                choose(index)
            }
        }

        onOpHeld1(BAG) { openBag() }
        onOpHeld2(BAG) { withdrawFirst() }
        onOpHeld3(BAG) { withdrawAll() }
        onOpHeld4(BAG) { resupply() }
        for (supply in ToaSupply.ALL_OBJS) {
            onOpHeldU(BAG, supply) { store(it.secondSlot) }
        }

        onIfOpen(BAG_IF) { player.startInvTransmit(player.bag) }
        onIfClose(BAG_IF) { player.stopInvTransmit(player.bag) }
        onIfModalButton(BAG_ITEMS) { bagButton(it) }
        onIfModalDrag(BAG_ITEMS) { rearrange(it) }
    }

    // ---- The spirit ----

    private fun ProtectedAccess.claim() {
        val supplies = player.currentRaid?.supplies ?: return
        if (!supplies.canClaim(player)) {
            mes(ALREADY_CLAIMED)
            return
        }
        for ((index, bundle) in supplies.bundles.withIndex()) {
            val inv = player.invMap.getOrPut(BUNDLE_INVS[index])
            for (slot in inv.indices) {
                val stack = bundle.getOrNull(slot)
                // One slot per kind with its count, as the bundle screen shows it.
                inv[slot] = stack?.let { InvObj(it.supply.obj(it.supply.maxDoses), it.count) }
            }
            UpdateInventory.updateInvFull(player, inv)
        }
        ifOpenMainModal(SELECT_IF)
        for (index in BUNDLE_INVS.indices) {
            val button = "component.toa_midraid_loot:select_button_${index + 1}"
            ifSetEvents(button, -1..-1, IfEvent.Op1)
        }
    }

    private suspend fun ProtectedAccess.choose(index: Int) {
        val supplies = player.currentRaid?.supplies ?: return
        val bundle = supplies.bundles.getOrNull(index) ?: return
        ifClose()
        if (!supplies.canClaim(player)) return
        val bag = player.bag
        val items = bundle.sumOf { it.count }
        if (BAG !in inv) {
            if (!inv.hasFreeSpace()) {
                mesbox(NEED_INV_SPACE)
                return
            }
            invClear(bag)
            invAdd(inv, BAG)
        } else if (bag.freeSpace() < items) {
            mesbox(NEED_BAG_SPACE)
            return
        }
        for (stack in bundle) {
            invAdd(bag, stack.supply.obj(stack.supply.maxDoses), stack.count, strict = false)
        }
        supplies.claimed(player)
    }

    // ---- The bag (item ops) ----

    private fun ProtectedAccess.openBag() {
        player.startInvTransmit(player.bag)
        ifOpenSide(BAG_IF)
        ifSetEvents(
            BAG_ITEMS,
            player.bag.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op9,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )
    }

    private fun ProtectedAccess.withdrawFirst() {
        val slot = player.bag.indices.firstOrNull { player.bag[it] != null } ?: return
        withdraw(slot)
    }

    private fun ProtectedAccess.withdrawAll() {
        for (slot in player.bag.indices) {
            if (player.bag[slot] == null) continue
            if (!withdraw(slot)) return
        }
    }

    /**
     * Tops up each partly used supply in the inventory with doses from the bag, taking the bag's
     * smallest items of that kind first.
     */
    private fun ProtectedAccess.resupply() {
        val bag = player.bag
        for (slot in inv.indices) {
            val held = ToaSupply.of(inv[slot]) ?: continue
            val supply = held.supply
            var doses = held.doses
            while (doses < supply.maxDoses) {
                val source =
                    bag.indices
                        .filter { ToaSupply.of(bag[it])?.supply == supply }
                        .minByOrNull { ToaSupply.of(bag[it])!!.doses } ?: break
                val available = ToaSupply.of(bag[source])!!.doses
                val taken = min(available, supply.maxDoses - doses)
                doses += taken
                bag[source] = if (available > taken) supply.invObj(available - taken) else null
            }
            if (doses != held.doses) inv[slot] = supply.invObj(doses)
        }
        disintegrateIfEmpty()
    }

    // ---- The bag (interface 778) ----

    private suspend fun ProtectedAccess.bagButton(event: IfModalButton) {
        val bag = player.bag
        val clicked = bag[event.comsub] ?: return
        when (event.op) {
            IfButtonOp.Op1 -> withdraw(event.comsub)
            IfButtonOp.Op2 -> withdrawKind(event.comsub, clicked.id, WITHDRAW_FIVE)
            IfButtonOp.Op3 -> withdrawKind(event.comsub, clicked.id, bag.size)
            IfButtonOp.Op4 -> {
                invDrop(event.comsub, bag)
                disintegrateIfEmpty()
            }
            IfButtonOp.Op9 -> objExamine(bag, event.comsub)
            else -> {}
        }
    }

    /** Up to [limit] items like the one in [from], starting at that slot. */
    private fun ProtectedAccess.withdrawKind(from: Int, objId: Int, limit: Int) {
        val bag = player.bag
        var taken = 0
        for (slot in (from until bag.size) + (0 until from)) {
            if (taken == limit) return
            if (bag[slot]?.id != objId) continue
            if (!withdraw(slot)) return
            taken++
        }
    }

    private fun ProtectedAccess.rearrange(event: IfModalDrag) {
        val bag = player.bag
        val from = event.selectedSlot ?: return
        val to = event.targetSlot ?: return
        if (from !in bag.indices || to !in bag.indices || from == to) return
        val moved = bag[from]
        bag[from] = bag[to]
        bag[to] = moved
    }

    // ---- Shared ----

    /**
     * Moves one item from the bag into the inventory. With a full inventory it only works for the
     * bag's last item, which then takes the bag's own slot (wiki).
     */
    private fun ProtectedAccess.withdraw(slot: Int): Boolean {
        val bag = player.bag
        val obj = bag[slot] ?: return false
        if (!inv.hasFreeSpace()) {
            if (bag.occupiedSpace() != 1) {
                mes(NO_INV_SPACE)
                return false
            }
            bag[slot] = null
            disintegrateIfEmpty()
            invAdd(inv, obj)
            return true
        }
        val moved = invMoveFromSlot(from = bag, into = inv, fromSlot = slot, count = obj.count)
        if (!moved[0].isOk()) {
            mes(NO_INV_SPACE)
            return false
        }
        disintegrateIfEmpty()
        return true
    }

    private fun ProtectedAccess.store(invSlot: Int) {
        val bag = player.bag
        if (!bag.hasFreeSpace()) {
            mes(BAG_FULL)
            return
        }
        val moved = invMoveFromSlot(from = inv, into = bag, fromSlot = invSlot, strict = false)
        if (!moved[0].isOk()) mes(BAG_FULL)
    }

    private fun ProtectedAccess.disintegrateIfEmpty() {
        if (player.bag.isNotEmpty() || BAG !in inv) return
        if (BAG_IF in player.ui) ifCloseSub(BAG_IF)
        invDel(inv, BAG, 1)
        mes("The bag disintegrates.")
    }

    private companion object {
        const val SPIRIT = "npc.toa_midraidloot_trader"
        const val SELECT_IF = "interface.toa_midraid_loot"
        const val BAG_IF = "interface.toa_midraidloot_bag"
        const val BAG_ITEMS = "component.toa_midraidloot_bag:items"
        const val BAG = "obj.toa_midraidloot_bag"
        const val BAG_INV = "inv.toa_midraidloot_bag"
        val BUNDLE_INVS =
            listOf(
                "inv.toa_midraidloot_bundle1",
                "inv.toa_midraidloot_bundle2",
                "inv.toa_midraidloot_bundle3",
            )

        const val WITHDRAW_FIVE = 5

        const val ALREADY_CLAIMED =
            "The spirit gives you a strange look. You've clearly claimed all you can for now."
        const val NEED_INV_SPACE = "You need at least one inventory spot for a supply bag."
        const val NEED_BAG_SPACE = "You need more space in your supply bag for additional supplies."
        const val NO_INV_SPACE =
            "You do not have enough space in your inventory to withdraw your supplies."
        const val BAG_FULL = "Your supply bag is full."
    }
}

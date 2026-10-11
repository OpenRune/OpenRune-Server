package org.rsmod.content.other.pouches.runepouch

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.util.UncheckedType
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The rune pouch and divine rune pouch.
 *
 * Runes live in the `rune_pouch_type_n` / `rune_pouch_quantity_n` varbits (three slots, four for
 * the divine pouch), which is what the spell system already reads when casting. The `rune_pouch`
 * interface draws itself from those varbits and the player inventory through its own cs2, so the
 * server only opens it, enables the slot ops and moves runes when a slot is clicked.
 */
@OptIn(UncheckedType::class)
class RunePouchScript
@Inject
constructor(private val objRepo: ObjRepository, private val protectedAccess: ProtectedAccessLauncher, private val bankScript: BankInvScript) :
    PluginScript() {
    private val loadouts = RunePouchLoadouts(bankScript)
    private var Player.quantityMode by intVarBit("varbit.rune_pouch_selectedquantity")
    private var Player.customQuantity by intVarBit("varbit.rune_pouch_customquantity")

    override fun ScriptContext.startup() {
        for (pouch in RunePouches.ALL) {
            onOpHeld1(pouch) { if (inv[it.slot]?.id == pouch.asRSCM()) openPouch() }
            onOpHeldU(pouch) { storeFromInventory(it.firstSlot, it.secondSlot) }
            onOpHeld4(pouch) { emptyPouch() }
            onProtectedEvent<HeldBanksideEvents.Operate>(pouch.asRSCM(RSCMType.OBJ)) {
                if (inv[it.slot]?.id == pouch.asRSCM() && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut) banksideOp()
            }
            onProtectedEvent<HeldBanksideEvents.Configure>(pouch.asRSCM(RSCMType.OBJ)) {
                if (inv[it.slot]?.id == pouch.asRSCM() && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut) loadouts.configure(this, it.slot) { openPouch() }
            }
        }
        onOpHeld3(RunePouches.DIVINE) { revertDivine(it.slot) }
        for (pouch in listOf(RunePouches.REGULAR, RunePouches.REGULAR_LOCKED)) {
            onOpHeldU(pouch, "obj.thread_of_elidinis") { upgrade(it.firstSlot, it.secondSlot) }
            for (needle in listOf("obj.needle", "obj.costumeneedle")) onOpHeldU(pouch, needle) {
                val thread = inv.indices.firstOrNull { slot -> inv[slot]?.id == "obj.thread_of_elidinis".asRSCM() } ?: return@onOpHeldU
                upgrade(it.firstSlot, thread)
            }
        }
        for (needle in listOf("obj.needle", "obj.costumeneedle")) onOpHeldU(needle, "obj.thread_of_elidinis") {
            val pouch = inv.indices.firstOrNull { slot -> inv[slot]?.id in listOf(RunePouches.REGULAR.asRSCM(), RunePouches.REGULAR_LOCKED.asRSCM()) } ?: return@onOpHeldU
            upgrade(pouch, it.secondSlot)
        }

        onIfModalButton(COMPONENT_POUCH) { pouchOp(it.comsub, it.op) }
        onIfModalButton(COMPONENT_INVENTORY) { inventoryOp(it.comsub, it.op) }
        onIfModalButton(COMPONENT_QUANTITY_1) { player.quantityMode = RunePouchQuantity.MODE_ONE }
        onIfModalButton(COMPONENT_QUANTITY_5) { player.quantityMode = RunePouchQuantity.MODE_FIVE }
        onIfModalButton(COMPONENT_QUANTITY_X) { selectCustomQuantity(it.op) }
        onIfModalButton(COMPONENT_QUANTITY_ALL) { player.quantityMode = RunePouchQuantity.MODE_ALL }

        onDestroyHeld {
            if (obj.id in RunePouches.ids && player.runePouchSlots() == null && player.invMap["inv.bank"]?.any { it?.id in RunePouches.ids } != true) {
                spillRunes(player)
            }
        }
    }

    private fun ProtectedAccess.openPouch() {
        ifOpenMainModal(INTERFACE)
        ifSetEvents(COMPONENT_POUCH, 0 until RunePouches.DIVINE_SLOTS, *SLOT_EVENTS)
        ifSetEvents(COMPONENT_INVENTORY, inv.indices, *SLOT_EVENTS)
    }

    /** A rune in the inventory panel: store it. Non-runes only carry a `Store` op that refuses. */
    private suspend fun ProtectedAccess.inventoryOp(slot: Int, op: IfButtonOp) {
        if (op == IfButtonOp.Op10) {
            objExamine(inv, slot)
            return
        }
        val obj = inv[slot] ?: return
        val slots = player.runePouchSlots()
        if (slots == null) {
            ifClose()
            return
        }
        val rune = RunePouchRunes.byObj(obj.id)
        if (rune == null) {
            val message =
                if (obj.id in RunePouches.ids) "Don't be silly." else "You can only store runes in the pouch."
            mes(message)
            return
        }
        val amount = resolveAmount(op) ?: return
        val current = inv[slot]
        if (current == null || current.id != obj.id) {
            return
        }
        val contents = player.readRunePouch(slots)
        val carried = inv.objs.sumOf { if (it != null && it.id == obj.id) it.count else 0 }
        val stored = contents.add(rune.compactId, minOf(amount, carried))
        if (stored == 0) {
            val message =
                if (contents.slotOf(rune.compactId) != -1) {
                    "Your pouch can't hold any more of that rune."
                } else {
                    "Your rune pouch is full."
                }
            mes(message)
            return
        }
        val removed = invDel(inv, rune.name, count = stored)
        if (removed.success) {
            player.writeRunePouch(contents)
        }
    }

    /** A rune in the pouch panel: withdraw it. */
    private suspend fun ProtectedAccess.pouchOp(slot: Int, op: IfButtonOp) {
        val slots = player.runePouchSlots()
        if (slots == null) {
            ifClose()
            return
        }
        if (slot !in 0 until slots) {
            return
        }
        val contents = player.readRunePouch(slots)
        val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: return
        if (contents.counts[slot] == 0) {
            return
        }
        if (op == IfButtonOp.Op10) {
            mes(rune.type.examine)
            return
        }
        val amount = resolveAmount(op) ?: return
        val latest = player.readRunePouch(slots)
        if (latest.types[slot] != rune.compactId || latest.counts[slot] == 0) {
            return
        }
        val take = minOf(amount, latest.counts[slot])
        val added = invAdd(inv, rune.name, count = take)
        if (added.failure) {
            mes("You don't have enough inventory space.")
            return
        }
        latest.remove(slot, take)
        player.writeRunePouch(latest)
    }

    private suspend fun ProtectedAccess.resolveAmount(op: IfButtonOp): Int? =
        when (val amount = RunePouchQuantity.resolve(op.ordinal + 1, player.quantityMode, player.customQuantity)) {
            is PouchAmount.Fixed -> amount.count
            PouchAmount.All -> Int.MAX_VALUE
            PouchAmount.Prompt -> promptQuantity()
            null -> null
        }

    /** Asks for a number, which also becomes the saved custom quantity shown on the `X` ops. */
    private suspend fun ProtectedAccess.promptQuantity(): Int? {
        val input = countDialog()
        if (input <= 0) {
            return null
        }
        player.customQuantity = input.coerceAtMost(RunePouchQuantity.MAX_CUSTOM)
        return input
    }

    /** Op1 selects the `X` quantity (asking for one if none is saved); op2 is "Set custom quantity". */
    private suspend fun ProtectedAccess.selectCustomQuantity(op: IfButtonOp) {
        if (op == IfButtonOp.Op2 || player.customQuantity == 0) {
            promptQuantity() ?: return
        }
        player.quantityMode = RunePouchQuantity.MODE_X
    }

    private fun ProtectedAccess.emptyPouch() {
        val slots = player.runePouchSlots() ?: return
        val contents = player.readRunePouch(slots)
        if (contents.isEmpty()) {
            mes("Your rune pouch is empty.")
            return
        }
        var partial = false
        for (slot in 0 until slots) {
            val count = contents.counts[slot]
            if (count == 0) {
                continue
            }
            val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: continue
            val added = invAdd(inv, rune.name, count = count)
            if (added.success) {
                contents.remove(slot, count)
            } else {
                partial = true
            }
        }
        player.writeRunePouch(contents)
        if (partial) {
            mes("You don't have enough inventory space to empty the whole pouch.")
        }
    }

    /**
     * The bank draws `Empty` (op9) and `Configure` (op1) on a pouch in the bankside inventory.
     * Emptying uses native bank deposits. Configure offers the pouch screen and saved loadouts.
     */
    private fun ProtectedAccess.banksideOp() {
        val slots = player.runePouchSlots() ?: return
        val contents = player.readRunePouch(slots)
        if (contents.isEmpty()) { mes("Your rune pouch is empty."); return }
        for (slot in 0 until slots) {
            val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: continue
            val moved = with(bankScript) { depositStoredStack(rune.id, contents.counts[slot]) }
            if (moved > 0) contents.remove(slot, moved)
        }
        player.writeRunePouch(contents)
        mes(if (contents.isEmpty()) "You empty the runes from your pouch into your bank." else "Your bank could not hold all the runes.")
    }

    private fun ProtectedAccess.storeFromInventory(pouchSlot: Int, runeSlot: Int) {
        if (inv[pouchSlot]?.id !in RunePouches.ids)return
        val obj = inv[runeSlot] ?: return
        val rune = RunePouchRunes.byObj(obj.id) ?: run { mes("You can only store runes in the pouch."); return }
        val slots = player.runePouchSlots() ?: return
        val contents = player.readRunePouch(slots)
        val amount = contents.add(rune.compactId, inv.physicalCount(rune.name))
        if (amount == 0) { mes("Your rune pouch has no room for those runes."); return }
        val tx = player.invDel(inv, rune.id, amount, autoCommit = false)
        if (tx.success) { tx.commitAll(); player.writeRunePouch(contents) }
    }

    private suspend fun ProtectedAccess.upgrade(pouchSlot: Int, threadSlot: Int) {
        val pouch = inv[pouchSlot] ?: return
        val thread = inv[threadSlot] ?: return
        if (pouch.id == RunePouches.REGULAR_LOCKED.asRSCM()) { mes("Ask Perdu to remove the lock before upgrading this pouch."); return }
        val result = when (pouch.id) { RunePouches.REGULAR.asRSCM() -> RunePouches.DIVINE; RunePouches.REGULAR_LOCKED.asRSCM() -> RunePouches.DIVINE_LOCKED; else -> return }
        if (thread.id != "obj.thread_of_elidinis".asRSCM())return
        fun ready() = player.craftingLvl >= 75 && ("obj.needle" in inv || "obj.costumeneedle" in inv)
        if (!ready()) { mes("You need Crafting 75 and a needle to stitch the pouch."); return }
        val origin = coords
        anim("seq.human_clay_stitching")
        try {
            delay(3)
            if (player.hitpoints == 0 || player.pendingLogout || player.loggingOut || coords != origin || inv[pouchSlot] !== pouch || inv[threadSlot] !== thread || !ready())return
            val tx = player.invTransaction(inv, autoCommit = false) { val i = select(inv); delete(i, pouch.id, 1, slot = pouchSlot); delete(i, thread.id, 1, slot = threadSlot); add(i, result.asRSCM(), 1, slot = pouchSlot) }
            if (tx.success) { tx.commitAll(); mes("You stitch the thread of Elidinis into your rune pouch.") }
        } finally { resetAnim() }
    }

    private suspend fun ProtectedAccess.revertDivine(slot: Int) {
        val obj = inv[slot] ?: return
        if (obj.id != RunePouches.DIVINE.asRSCM())return
        val confirm = choice2("Yes, return the pouch and thread. Any runes in slot four will be lost.", true, "No, keep it.", false, title = "Revert the divine rune pouch?")
        if (!confirm || inv[slot] !== obj || player.hitpoints == 0 || player.pendingLogout || player.loggingOut) return
        val contents = player.readRunePouch(RunePouches.DIVINE_SLOTS)
        val tx = player.invTransaction(inv, autoCommit = false) { val i = select(inv); delete(i, obj.id, 1, slot = slot); add(i, RunePouches.REGULAR.asRSCM(), 1, slot = slot); add(i, "obj.thread_of_elidinis".asRSCM(), 1) }
        if (tx.failure) { mes("You need one free inventory slot to receive the thread."); return }
        tx.commitAll()
        contents.remove(RunePouches.DIVINE_SLOTS - 1, Int.MAX_VALUE)
        player.writeRunePouch(contents)
        mes("You recover the rune pouch and thread of Elidinis.")
    }

    /** "If you destroy the pouch, all the contents will fall out." */
    private fun spillRunes(player: Player) {
        val contents = player.readRunePouch(RunePouches.DIVINE_SLOTS)
        for (slot in 0 until contents.slots) {
            val count = contents.counts[slot]
            if (count == 0) {
                continue
            }
            val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: continue
            val obj = Obj.fromOwner(player, player.coords, InvObj(rune.id, count))
            objRepo.add(obj, SPILL_DURATION)
        }
        contents.clear()
        player.writeRunePouch(contents)
    }

    private companion object {
        private const val INTERFACE = "interface.rune_pouch"
        private const val COMPONENT_POUCH = "component.rune_pouch:pouch"
        private const val COMPONENT_INVENTORY = "component.rune_pouch:inventory"
        private const val COMPONENT_QUANTITY_1 = "component.rune_pouch:1"
        private const val COMPONENT_QUANTITY_5 = "component.rune_pouch:5"
        private const val COMPONENT_QUANTITY_X = "component.rune_pouch:x"
        private const val COMPONENT_QUANTITY_ALL = "component.rune_pouch:all"

        /** Ticks the spilled runes of a destroyed pouch stay on the floor. */
        private const val SPILL_DURATION = 300

        private val SLOT_EVENTS =
            arrayOf(
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op6,
                IfEvent.Op10,
            )
    }
}

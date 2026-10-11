package org.rsmod.content.other.containers

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.content.skills.magic.spell.teleports.TeleportScroll
import org.rsmod.content.skills.magic.spell.teleports.TeleportScrollTravel
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class MasterScrollBookScript @Inject constructor(private val travel: TeleportScrollTravel) : PluginScript() {
    override fun ScriptContext.startup() {
        for (book in MasterScrollBook.forms) {
            onOpHeld3(book) { if (inv[it.slot]?.id == book.asRSCM())open() }
            for (scroll in TeleportScroll.entries)onOpHeldU(book, scroll.item) { fill(book, it.firstSlot, scroll, it.secondSlot) }
        }
        onOpHeld1(MasterScrollBook.FILLED) {
            val scroll = TeleportScroll.entries.getOrNull(MasterScrollBook.defaultId(player) - 1)
            if (scroll == null)open()else activate(scroll)
        }
        onOpHeld4(MasterScrollBook.FILLED) { MasterScrollBook.setDefault(player, 0); mes("You remove the default teleport.") }
        for (scroll in TeleportScroll.entries) {
            onIfModalButton("component.bookofscrolls:teleportscroll_${MasterScrollBook.suffix(scroll)}") {
                if (!MasterScrollBook.owns(player)) { ifClose(); return@onIfModalButton }
                when (it.op) {
                    IfButtonOp.Op1 -> activate(scroll)
                    IfButtonOp.Op2 -> { MasterScrollBook.setDefault(player, scroll.ordinal + 1); mes("Default teleport set to ${scroll.name.replace(Regex("([a-z])([A-Z])"),"$1 $2")}.") }
                    IfButtonOp.Op3 -> remove(scroll)
                    IfButtonOp.Op4 -> if (scroll == TeleportScroll.Revenants) {
                        val skip = choice2("Show Wilderness warning", false, "Disable Wilderness warning", true)
                        ifCloseChat(); VarPlayerIntMapSetter.set(player, "varp.revenant_scroll_nowarn", if (skip)1 else 0)
                    }
                    else -> Unit
                }
            }
        }
        onIfModalButton("component.bookofscrolls:close") { ifClose() }
    }
    private fun ProtectedAccess.open() {
        if (!MasterScrollBook.owns(player))return
        ifOpenMainModal("interface.bookofscrolls")
        for (scroll in TeleportScroll.entries)ifSetEvents("component.bookofscrolls:teleportscroll_${MasterScrollBook.suffix(scroll)}", -1..-1,
            *if (scroll == TeleportScroll.Revenants)arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4)else arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3))
    }
    private fun ProtectedAccess.fill(book: String, bookSlot: Int, scroll: TeleportScroll, scrollSlot: Int) {
        if (inv[bookSlot]?.id != book.asRSCM() || inv[scrollSlot]?.id != scroll.item.asRSCM())return
        val old = MasterScrollBook.count(player, scroll); val n = minOf(MasterScrollBook.CAPACITY - old, inv.physicalCount(scroll.item))
        if (n <= 0) { mes("The book already contains 1,000 of those scrolls."); return }
        val tx = player.invTransaction(inv, autoCommit = false) {
            val i = select(inv); delete(i, scroll.item.asRSCM(), n)
            if (book == MasterScrollBook.EMPTY) { delete(i, book.asRSCM(), 1, slot = bookSlot); add(i, MasterScrollBook.FILLED.asRSCM(), 1, slot = bookSlot) }
        }
        if (tx.success) { tx.commitAll(); MasterScrollBook.setCount(player, scroll, old + n) }
    }
    private suspend fun ProtectedAccess.remove(scroll: TeleportScroll) {
        if (MasterScrollBook.count(player, scroll) == 0) { mes("There are no scrolls of that type in your book."); return }
        val n = countDialog(); ifCloseChat()
        if (n <= 0 || !MasterScrollBook.owns(player))return
        val current = MasterScrollBook.count(player, scroll); val take = minOf(current, n)
        if (take == 0)return
        val tx = player.invAdd(inv, scroll.item.asRSCM(), take, autoCommit = false)
        if (tx.failure) { mes("You need inventory space to remove those scrolls."); return }
        tx.commitAll(); MasterScrollBook.setCount(player, scroll, current - take); updateEmpty()
    }
    private suspend fun ProtectedAccess.activate(scroll: TeleportScroll) {
        if (!MasterScrollBook.owns(player) || MasterScrollBook.count(player, scroll) <= 0) { mes("Your book has no scrolls for that teleport."); return }
        ifClose()
        travel.travel(this, scroll, valid = { MasterScrollBook.owns(player) && MasterScrollBook.count(player, scroll) > 0 }, consume = {
            if (!MasterScrollBook.owns(player) || MasterScrollBook.count(player, scroll) <= 0)false
            else { MasterScrollBook.setCount(player, scroll, MasterScrollBook.count(player, scroll) - 1); updateEmpty(); true }
        })
    }
    private fun ProtectedAccess.updateEmpty() {
        if (MasterScrollBook.total(player) > 0)return
        MasterScrollBook.setDefault(player, 0)
        for (slot in inv.indices)if (inv[slot]?.id == MasterScrollBook.FILLED.asRSCM())player.invTransaction(inv) { val i = select(inv); delete(i, MasterScrollBook.FILLED.asRSCM(), 1, slot = slot); add(i, MasterScrollBook.EMPTY.asRSCM(), 1, slot = slot) }
    }
}

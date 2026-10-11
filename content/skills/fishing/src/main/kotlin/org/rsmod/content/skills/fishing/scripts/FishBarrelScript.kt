package org.rsmod.content.skills.fishing.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.*
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.skills.fishing.FishBarrelContents
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FishBarrelScript @Inject constructor(private val bankScript: BankInvScript) : PluginScript() {
    override fun ScriptContext.startup() {
        for (symbol in FishBarrelContents.forms) {
            val type = ServerCacheManager.getItem(symbol.asRSCM())!!
            for (op in 1..4) {
                val action = type.interfaceOptions[op - 1]
                if (action !in setOf("Fill", "Check", "Configure", "Open", "Close"))continue
                when (op) {
                    1 -> onOpHeld1(symbol) { dispatch(symbol, it.slot, action!!) }
                    2 -> onOpHeld2(symbol) { dispatch(symbol, it.slot, action!!) }
                    3 -> onOpHeld3(symbol) { dispatch(symbol, it.slot, action!!) }
                    4 -> onOpHeld4(symbol) { dispatch(symbol, it.slot, action!!) }
                }
            }
            onProtectedEvent<HeldBanksideEvents.Operate>(type.id) { if (player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && inv[it.slot]?.id == type.id) emptyIntoBank() }
        }
        for (symbol in listOf(SACK_CLOSED, SACK_OPEN)) {
            onOpWorn2(symbol) { checkContents() }
            onOpWorn3(symbol) { toggle(symbol, it.slot, player.worn) }
        }
        onOpHeldU(FISH_SACK, BARREL_CLOSED) { combine(BARREL_CLOSED, SACK_CLOSED, it.firstSlot, it.secondSlot) }
        onOpHeldU(FISH_SACK, BARREL_OPEN) { combine(BARREL_OPEN, SACK_OPEN, it.firstSlot, it.secondSlot) }
        onEvent<HeldDropEvents.Destroy> {
            if (obj.id in FishBarrelContents.forms.map { it.asRSCM() } && !FishBarrelContents.owns(player))player.attr.remove(FishBarrelContents.key)
        }
    }
    private suspend fun ProtectedAccess.dispatch(symbol: String, slot: Int, action: String) {
        if (inv[slot]?.id != symbol.asRSCM())return
        when (action) {
            "Fill" -> fill()
            "Check" -> checkContents()
            "Open", "Close" -> toggle(symbol, slot, inv)
            "Configure" -> when (menu("Fish sack barrel", hotkeys = true, choices = listOf("Check contents", "Dismantle", "Cancel"))) {
                0 -> checkContents()
                1 -> if (inv[slot]?.id == symbol.asRSCM())dismantle(symbol, slot)
            }
        }
    }
    private fun ProtectedAccess.fill() {
        var moved = 0
        for (item in FishBarrelContents.allowed) {
            val room = FishBarrelContents.CAPACITY - FishBarrelContents.total(player)
            val n = minOf(room, inv.physicalCount(item)); if (n <= 0)continue
            val tx = player.invDel(inv, item.asRSCM(), n, autoCommit = false)
            if (tx.failure)continue
            tx.commitAll(); FishBarrelContents.update(player, item, n); moved += n
        }
        mes(if (moved > 0)"You fill the fish barrel."else "You have no suitable raw fish, or the barrel is full.")
    }
    private fun ProtectedAccess.checkContents() {
        val items = FishBarrelContents.contents(player)
        mes(if (items.isEmpty())"The fish barrel is empty."else "Fish barrel (${FishBarrelContents.total(player)}/28): " + items.entries.joinToString(", ") { (symbol, n) -> "${ServerCacheManager.getItem(symbol.asRSCM())!!.name} x $n" })
    }
    private fun ProtectedAccess.toggle(symbol: String, slot: Int, target: Inventory) {
        if (target[slot]?.id != symbol.asRSCM())return
        val other = when (symbol) { BARREL_CLOSED -> BARREL_OPEN; BARREL_OPEN -> BARREL_CLOSED; SACK_CLOSED -> SACK_OPEN; else -> SACK_CLOSED }
        player.invTransaction(target) { val i = select(target); delete(i, symbol.asRSCM(), 1, slot = slot); add(i, other.asRSCM(), 1, slot = slot) }
    }
    private fun ProtectedAccess.combine(barrel: String, result: String, sackSlot: Int, barrelSlot: Int) {
        if (inv[sackSlot]?.id != FISH_SACK.asRSCM() || inv[barrelSlot]?.id != barrel.asRSCM())return
        player.invTransaction(inv) { val i = select(inv); delete(i, FISH_SACK.asRSCM(), 1, slot = sackSlot); delete(i, barrel.asRSCM(), 1, slot = barrelSlot); add(i, result.asRSCM(), 1, slot = barrelSlot) }
    }
    private fun ProtectedAccess.dismantle(symbol: String, slot: Int) {
        val barrel = if (symbol == SACK_OPEN)BARREL_OPEN else BARREL_CLOSED
        val tx = player.invTransaction(inv, autoCommit = false) { val i = select(inv); delete(i, symbol.asRSCM(), 1, slot = slot); add(i, barrel.asRSCM(), 1, slot = slot); add(i, FISH_SACK.asRSCM(), 1) }
        if (tx.failure) { mes("You need one free inventory slot."); return }; tx.commitAll()
    }
    private fun ProtectedAccess.emptyIntoBank() {
        var moved = 0
        for ((item, n) in FishBarrelContents.contents(player).toMap()) {
            val actual = with(bankScript) { depositStoredStack(item.asRSCM(), n) }
            if (actual > 0) { FishBarrelContents.update(player, item, -actual); moved += actual }
        }
        mes(if (moved > 0) "You empty fish from the barrel into your bank." else "Your bank has no room, or the barrel is empty.")
    }
    private companion object {
        const val BARREL_CLOSED = "obj.fish_barrel_closed"; const val BARREL_OPEN = "obj.fish_barrel_open"
        const val SACK_CLOSED = "obj.fish_sack_barrel_closed"; const val SACK_OPEN = "obj.fish_sack_barrel_open"; const val FISH_SACK = "obj.fish_sack"
    }
}

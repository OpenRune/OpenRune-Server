package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ForestryBasketScript @Inject constructor(private val store: ContainerStore, private val bankScript: BankInvScript, private val kitScript: ForestryKitScript) : PluginScript() {
    override fun ScriptContext.startup() {
        for (item in listOf(ForestryContainers.KIT, "obj.log_basket_closed", "obj.log_basket_open")) onOpHeldU(item, ForestryContainers.HARNESS) { combine(item, it.firstSlot, it.secondSlot) }
        for (form in ForestryContainers.baskets) {
            onOpHeld1(form) { operate(it.slot, "Fill") }
            onOpHeld3(form) { operate(it.slot, "View") }
            onOpHeld4(form) { operate(it.slot, "Configure") }
            onOpHeldSubOp(form) {
                if (it.inventory[it.slot] !== it.obj) return@onOpHeldSubOp
                val action = when (it.op.ordinal + 1) {
                    1 -> listOf("Fill kit", "Fill logs").getOrNull(it.subop)
                    3 -> listOf("View kit", "Empty logs").getOrNull(it.subop)
                    4 -> listOf("Toggle", "Dismantle").getOrNull(it.subop)
                    else -> null
                }
                if (action != null) operate(it.slot, action, it.inventory)
            }
            onOpHeldU(form) { if (inv[it.firstSlot]?.id == form.asRSCM() && inv[it.secondSlot]?.id == it.second.id) { val spec = if (it.second.id in ForestryContainers.logs.accepted) ForestryContainers.logs else ForestryContainers.kit; val moved = store.store(player, spec, it.second.id, Int.MAX_VALUE); mes(if (moved > 0) "You store $moved items." else "That item cannot be stored here.") } }
            onOpWornSubOp(form) { if (worn[it.slot] === it.obj) wornSubOp(it.obj.id, it.op, it.subop) }
            onOpWorn2(form) { operate(it.slot, "Fill", worn) }
            onOpWorn3(form) { operate(it.slot, "View", worn) }
            onOpWorn4(form) { operate(it.slot, "Toggle", worn) }
            onProtectedEvent<HeldBanksideEvents.Operate>(form.asRSCM()) {
                if (!valid(inv, it.slot)) return@onProtectedEvent
                val logs = store.contents(player, ForestryContainers.logs).toMap()
                if (logs.isNotEmpty()) deposit(ForestryContainers.logs, logs)
                else {
                    val leaves = store.contents(player, ForestryContainers.kit).filterKeys { id -> id.toInt() in ForestryContainers.leaves }
                    if (leaves.isNotEmpty()) deposit(ForestryContainers.kit, leaves)
                    else for (id in ForestryContainers.leaves) {
                        val amount = with(bankScript) { withdrawStoredStack(id, store.room(player, ForestryContainers.kit, id)) }
                        if (amount > 0) store.update(player, ForestryContainers.kit, id, amount)
                    }
                }
            }
        }
        onDestroyHeld {
            if (type.asRSCM() !in ForestryContainers.basketIds) return@onDestroyHeld
            if (!store.owns(player, ForestryContainers.kit)) ForestryContainers.destroyLastKit(player, store)
            if (ContainerCatalog.all.none { it.family == "log_basket" && store.owns(player, it) }) store.clear(player, ForestryContainers.logs)
        }
    }
    private suspend fun ProtectedAccess.wornSubOp(item: Int?, op: IfButtonOp, subop: Int) {
        val slot = Wearpos.Back.slot
        if (item == null || worn[slot]?.id != item || !valid(worn, slot)) return
        val action = when (op) { IfButtonOp.Op2 -> listOf("Fill kit", "Fill logs").getOrNull(subop); IfButtonOp.Op3 -> listOf("View kit", "Empty logs").getOrNull(subop); else -> null }
        if (action != null) operate(slot, action, worn)
    }
    private fun ProtectedAccess.valid(source: Inventory, slot: Int): Boolean = source[slot]?.id in ForestryContainers.basketIds && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    private fun ProtectedAccess.deposit(spec: ContainerSpec, contents: Map<String, Int>) {
        for ((id, count) in contents) { val moved = with(bankScript) { depositStoredStack(id.toInt(), count) }; if (moved > 0) store.update(player, spec, id.toInt(), -moved) }
    }
    private suspend fun ProtectedAccess.operate(slot: Int, action: String, source: Inventory = inv) {
        if (!valid(source, slot)) return
        val reference = source[slot]; val origin = coords
        val chosen = when (action) {
            "Fill" -> listOf("Fill kit", "Fill logs", "Cancel").getOrNull(menu("Fill which container?", "Forestry kit", "Log basket", "Cancel", hotkeys = true)) ?: "Cancel"
            "View" -> listOf("View kit", "Empty logs", "Cancel").getOrNull(menu("View which container?", "Forestry kit", "Empty basket", "Cancel", hotkeys = true)) ?: "Cancel"
            "Configure" -> listOf("Toggle", "Dismantle", "Cancel").getOrNull(menu("Configure basket", "Open/Close", "Dismantle", "Cancel", hotkeys = true)) ?: "Cancel"
            else -> action
        }
        if (!valid(source, slot) || source[slot] !== reference || coords != origin) return
        val spec = if (chosen.endsWith("logs")) ForestryContainers.logs else ForestryContainers.kit
        when (chosen) {
            "Fill kit", "Fill logs" -> { var moved = 0L; for (item in inv.filterNotNull().toList()) moved += store.store(player, spec, item.id, item.count); mes(if (moved > 0) "You store $moved items." else "No suitable items could be stored.") }
            "Empty logs" -> { for ((id, count) in store.contents(player, spec).toMap()) store.withdraw(player, spec, id.toInt(), count, inv); mes(if (store.contents(player, spec).isEmpty()) "You empty the basket." else "You empty as many logs as you can carry.") }
            "View kit" -> kitScript.open(this)
            "Toggle" -> {
                val target = if (reference!!.id == ForestryContainers.OPEN.asRSCM()) ForestryContainers.CLOSED else ForestryContainers.OPEN
                val tx = player.invTransaction(source) { val i = select(source); delete(i, reference.id, 1, slot = slot); add(i, target.asRSCM(), 1, slot = slot) }
                if (tx.success && source === worn) rebuildAppearance()
            }
            "Dismantle" -> {
                val log = if (reference!!.id == ForestryContainers.OPEN.asRSCM()) "obj.log_basket_open" else "obj.log_basket_closed"
                val tx = player.invTransaction(source, inv) { val from = select(source); val into = if (source === inv) from else select(inv); delete(from, reference.id, 1, slot = slot); add(into, ForestryContainers.KIT.asRSCM(), 1); add(into, log.asRSCM(), 1); add(into, ForestryContainers.HARNESS.asRSCM(), 1) }
                if (tx.failure) mes("You need room for the kit, log basket and harness.") else if (source === worn) rebuildAppearance()
            }
        }
    }
    private fun ProtectedAccess.combine(used: String, slot: Int, harnessSlot: Int) {
        if (inv[slot]?.id != used.asRSCM() || inv[harnessSlot]?.id != ForestryContainers.HARNESS.asRSCM() || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        val log = if (used.startsWith("obj.log_basket")) used else listOf("obj.log_basket_open", "obj.log_basket_closed").firstOrNull { inv.physicalCount(it) > 0 } ?: return
        val target = if (log.endsWith("_open")) ForestryContainers.OPEN else ForestryContainers.CLOSED
        val tx = player.invTransaction(inv) { val i = select(inv); delete(i, ForestryContainers.KIT.asRSCM(), 1); delete(i, log.asRSCM(), 1); delete(i, ForestryContainers.HARNESS.asRSCM(), 1); add(i, target.asRSCM(), 1) }
        if (tx.success) mes("You combine your log basket, forestry kit and sturdy harness.")
    }
}

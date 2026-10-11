package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ContainerScript @Inject constructor(private val store: ContainerStore, private val bankScript: BankInvScript) : PluginScript() {
    override fun ScriptContext.startup() {
        onDestroyHeld {
            val spec = ContainerCatalog.byId[type.asRSCM()] ?: return@onDestroyHeld
            val remaining = ContainerCatalog.all.any { it.family == spec.family && store.owns(player, it) } || player.invMap["inv.bank"]?.any { item -> item != null && (ContainerCatalog.byId[item.id]?.family == spec.family || ForestryContainers.ownsFamily(item.id, spec)) } == true
            if (!remaining) { if (spec.family == "forestry_kit") ForestryContainers.destroyLastKit(player, store) else store.clear(player, spec) }
            if (!remaining && spec.family == "seed_box") player.attr.remove(SeedBox.ORDER)
        }
        for (spec in ContainerCatalog.all) {
            val type = ServerCacheManager.getItem(spec.id)!!
            for (op in 1..5) {
                val action = type.interfaceOptions[op - 1] ?: continue
                if (spec.family == "seed_box" && action == "Check") continue
                if (action !in ACTIONS || spec.family in setOf("tackle_box", "huntsman_kit", "forestry_kit") && action == "View") continue
                when (op) {
                    1 -> onOpHeld1(spec.symbol) { operate(spec, it.slot, action) }
                    2 -> onOpHeld2(spec.symbol) { operate(spec, it.slot, action) }
                    3 -> onOpHeld3(spec.symbol) { operate(spec, it.slot, action) }
                    4 -> onOpHeld4(spec.symbol) { operate(spec, it.slot, action) }
                    5 -> onOpHeld5(spec.symbol) { operate(spec, it.slot, action) }
                }
            }
            val wornOps = listOf(params.wear_op1, params.wear_op2, params.wear_op3, params.wear_op4, params.wear_op5, params.wear_op6, params.wear_op7, params.wear_op8)
            for ((index, param) in wornOps.withIndex()) {
                val action = type.paramOrNull(param) ?: continue
                if (action !in ACTIONS || spec.family == "forestry_kit" && action == "View") continue
                when (index + 2) {
                    2 -> onOpWorn2(spec.symbol) { operate(spec, it.slot, action, worn) }
                    3 -> onOpWorn3(spec.symbol) { operate(spec, it.slot, action, worn) }
                    4 -> onOpWorn4(spec.symbol) { operate(spec, it.slot, action, worn) }
                    5 -> onOpWorn5(spec.symbol) { operate(spec, it.slot, action, worn) }
                    6 -> onOpWorn6(spec.symbol) { operate(spec, it.slot, action, worn) }
                    7 -> onOpWorn7(spec.symbol) { operate(spec, it.slot, action, worn) }
                    8 -> onOpWorn8(type) { operate(spec, it.slot, action, worn) }
                    9 -> onOpWorn9(type) { operate(spec, it.slot, action, worn) }
                }
            }
            onOpHeldU(spec.symbol) {
                if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return@onOpHeldU
                if (inv[it.firstSlot]?.id != spec.id || inv[it.secondSlot]?.id != it.second.id) return@onOpHeldU
                val moved = store.store(player, spec, it.second.id, Int.MAX_VALUE)
                mes(if (moved > 0) "You store $moved items." else "That item doesn't fit in this container.")
            }
            onProtectedEvent<HeldBanksideEvents.Operate>(spec.id) {
                if (player.pendingLogout || player.loggingOut || player.hitpoints <= 0 || inv[it.slot]?.id != spec.id || !store.owns(player, spec)) return@onProtectedEvent
                val fill = spec.family in setOf("tackle_box", "huntsman_kit") || spec.family == "plank_sack" && inv.objs.any { obj -> obj != null && obj.id in spec.accepted }
                if (fill) {
                    var moved = 0L
                    for (obj in inv.objs.filterNotNull().toList()) moved += store.store(player, spec, obj.id, obj.count)
                    mes(if (moved > 0) "You store $moved items." else "No suitable items could be stored.")
                } else {
                    var moved = 0L
                    for ((id, count) in orderedContents(spec)) {
                        moved += if (spec.family in setOf("forestry_kit", "satchel", "shade_coffin", "flamtaer_bag")) store.withdraw(player, spec, id.toInt(), count, inv) else bankContents(spec, id.toInt(), count)
                    }
                    mes(if (moved > 0) "You withdraw $moved stored items." else "No stored items could be withdrawn.")
                }
            }
        }
    }
    private fun ProtectedAccess.bankContents(spec: ContainerSpec, id: Int, count: Int): Int {
        val name = store.inventoryName(spec)
        if (name != null) {
            val storage = player.invMap[name] ?: return 0
            var moved = 0
            for (slot in storage.indices) {
                val item = storage[slot]?.takeIf { it.id == id } ?: continue
                with(bankScript) { invDeposit(slot, item.count, storage) }
                moved += item.count - (storage[slot]?.count ?: 0)
            }
            return moved
        }
        val moved = with(bankScript) { depositStoredStack(id, count) }
        if (moved > 0) store.update(player, spec, id, -moved)
        return moved
    }
    private suspend fun ProtectedAccess.operate(spec: ContainerSpec, slot: Int, action: String, container: Inventory = inv) {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || container[slot]?.id != spec.id) return
        when (action) {
            "Fill" -> {
                var count = 0L
                for (obj in inv.objs.filterNotNull().toList()) count += store.store(player, spec, obj.id, obj.count)
                mes(if (count > 0) "You store $count items." else "You have no suitable items or the container is full.")
            }
            "Empty" -> {
                if (spec.bankOnly) { mes("Withdraw these contents at a bank."); return }
                var moved = 0L
                for ((id, count) in orderedContents(spec)) moved += store.withdraw(player, spec, id.toInt(), count, inv)
                mes(if (store.contents(player, spec).isNotEmpty()) "Your inventory cannot hold all the contents." else "You empty the container.")
            }
            "Open", "Close" -> {
                val prefix = spec.symbol.removeSuffix("_open").removeSuffix("_closed")
                val other = ContainerCatalog.all.firstOrNull { it.family == spec.family && it.id != spec.id && it.symbol.removeSuffix("_open").removeSuffix("_closed") == prefix }
                if (other == null) { view(spec); return }
                val change = player.invTransaction(container) { val i = select(container); delete(i, spec.id, 1, slot = slot); add(i, other.id, 1, slot = slot) }
                if (change.success && container === worn) rebuildAppearance()
                if (change.success) mes(if (other.opened) "You open the container." else "You close the container.")
            }
            "Configure" -> {
                if (spec.family != "shade_coffin") { view(spec); return }
                val reference = container[slot]
                when (menu("Configure coffin", hotkeys = true, choices = listOf("Check Shade Count", if (spec.opened) "Close coffin" else "Open coffin", "Remove lock", "Cancel"))) {
                    0 -> if (container[slot] === reference) operate(spec, slot, "Check", container)
                    1 -> if (container[slot] === reference) operate(spec, slot, if (spec.opened) "Close" else "Open", container)
                    2 -> {
                        val yes = menu("Remove the lock? Stored shade remains will be lost.", hotkeys = true, choices = listOf("Yes", "No")) == 0
                        if (!yes || container[slot] !== reference || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
                        val lock = ShadeCoffins.lockFor(spec.symbol) ?: return
                        val tx = player.invTransaction(container, inv) { val source = select(container); val target = if (container === inv) source else select(inv); delete(source, spec.id, 1, slot = slot); add(target, ShadeCoffins.BROKEN.asRSCM(), 1); add(target, lock.asRSCM(), 1) }
                        if (tx.success) { store.clear(player, spec); if (container === worn) rebuildAppearance(); mes("You remove the lock and lose the stored remains.") } else mes("You need space for the broken coffin and lock.")
                    }
                }
            }
            "Check", "Inspect", "Look-in" -> {
                val items = store.contents(player, spec)
                mes(if (items.isEmpty()) "This container is empty." else items.entries.joinToString("<br>") { (id, count) -> "${ServerCacheManager.getItem(id.toInt())?.name}: ${"%,d".format(count)}" })
            }
            else -> view(spec)
        }
    }
    private fun ProtectedAccess.orderedContents(spec: ContainerSpec): Map<String, Int> =
        if (spec.family == "shade_coffin") store.contents(player, spec).toSortedMap(compareByDescending { it.toInt() }) else store.contents(player, spec).toMap()
    private suspend fun ProtectedAccess.view(spec: ContainerSpec) {
        val items = store.contents(player, spec).entries.toList()
        if (items.isEmpty()) { mes("This container is empty."); return }
        val pages = items.chunked(8)
        var page = 0
        while (store.owns(player, spec)) {
            val slice = pages[page]
            val labels = slice.map { (id, count) -> "${ServerCacheManager.getItem(id.toInt())?.name ?: "Item"}: ${"%,d".format(count)}" }
            val index = menu("Stored items (${page + 1}/${pages.size})", hotkeys = true, choices = labels + listOf("Next page", "Close"))
            when {
                index in slice.indices -> {
                    if (spec.bankOnly) { mes("Withdraw these contents at a bank."); return }
                    val item = slice[index]
                    val amount = countDialog()
                    if (amount > 0) store.withdraw(player, spec, item.key.toInt(), amount, inv)
                    return
                }
                index == slice.size -> page = (page + 1) % pages.size
                else -> return
            }
        }
    }
    private companion object { val ACTIONS = setOf("Fill", "Empty", "Open", "Close", "Check", "Inspect", "View", "Configure", "Look-in") }
}

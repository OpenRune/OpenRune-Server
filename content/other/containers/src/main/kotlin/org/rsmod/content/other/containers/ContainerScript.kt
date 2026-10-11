package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ContainerScript @Inject constructor(private val store: ContainerStore) : PluginScript() {
    override fun ScriptContext.startup() {
        onDestroyHeld {
            val spec = ContainerCatalog.byId[type.asRSCM()] ?: return@onDestroyHeld
            val remaining = ContainerCatalog.all.any { it.family == spec.family && store.owns(player, it) }
            if (!remaining) store.clear(player, spec)
        }
        for (spec in ContainerCatalog.all) {
            val type = ServerCacheManager.getItem(spec.id)!!
            for (op in 1..5) {
                val action = type.interfaceOptions[op - 1] ?: continue
                if (action !in ACTIONS || spec.family in setOf("tackle_box", "huntsman_kit") && action == "View") continue
                when (op) {
                    1 -> onOpHeld1(spec.symbol) { operate(spec, it.slot, action) }
                    2 -> onOpHeld2(spec.symbol) { operate(spec, it.slot, action) }
                    3 -> onOpHeld3(spec.symbol) { operate(spec, it.slot, action) }
                    4 -> onOpHeld4(spec.symbol) { operate(spec, it.slot, action) }
                    5 -> onOpHeld5(spec.symbol) { operate(spec, it.slot, action) }
                }
            }
            onOpHeldU(spec.symbol) {
                if (inv[it.firstSlot]?.id != spec.id || inv[it.secondSlot]?.id != it.second.id) return@onOpHeldU
                val moved = store.store(player, spec, it.second.id, Int.MAX_VALUE)
                mes(if (moved > 0) "You store $moved items." else "That item doesn't fit in this container.")
            }
            onEvent<HeldBanksideEvents.Type>(spec.id) {
                if (!store.owns(player, spec)) return@onEvent
                val fill = spec.family in setOf("tackle_box", "huntsman_kit") || spec.family == "plank_sack" && player.inv.objs.any { it != null && it.id in spec.accepted }
                if (fill) {
                    var moved = 0L
                    for (obj in player.inv.objs.filterNotNull().toList()) moved += store.store(player, spec, obj.id, obj.count)
                    player.mes(if (moved > 0) "You store $moved items." else "No suitable items could be stored.")
                    return@onEvent
                }
                val target = if (spec.family in setOf("forestry_kit", "satchel", "shade_coffin", "flamtaer_bag")) player.inv else player.invMap.getOrPut("inv.bank")
                var moved = 0L
                for ((id, count) in store.contents(player, spec).toMap()) moved += store.withdraw(player, spec, id.toInt(), count, target)
                player.mes(if (moved > 0) "You withdraw $moved stored items." else "No stored items could be withdrawn.")
            }
        }
    }
    private suspend fun ProtectedAccess.operate(spec: ContainerSpec, slot: Int, action: String) {
        if (inv[slot]?.id != spec.id) return
        when (action) {
            "Fill" -> {
                var count = 0L
                for (obj in inv.objs.filterNotNull().toList()) count += store.store(player, spec, obj.id, obj.count)
                mes(if (count > 0) "You store $count items." else "You have no suitable items or the container is full.")
            }
            "Empty" -> {
                if (spec.bankOnly) { mes("Withdraw these contents at a bank."); return }
                var moved = 0L
                for ((id, count) in store.contents(player, spec).toMap()) moved += store.withdraw(player, spec, id.toInt(), count, inv)
                mes(if (store.contents(player, spec).isNotEmpty()) "Your inventory cannot hold all the contents." else "You empty the container.")
            }
            "Open", "Close" -> {
                val prefix = spec.symbol.removeSuffix("_open").removeSuffix("_closed")
                val other = ContainerCatalog.all.firstOrNull { it.family == spec.family && it.id != spec.id && it.symbol.removeSuffix("_open").removeSuffix("_closed") == prefix }
                if (other == null) { view(spec); return }
                val change = player.invTransaction(inv) { val i = select(inv); delete(i, spec.id, 1, slot = slot); add(i, other.id, 1, slot = slot) }
                if (change.success) mes(if (other.opened) "You open the container." else "You close the container.")
            }
            "Check", "Inspect" -> {
                val items = store.contents(player, spec)
                mes(if (items.isEmpty()) "This container is empty." else items.entries.joinToString("<br>") { (id, count) -> "${ServerCacheManager.getItem(id.toInt())?.name}: ${"%,d".format(count)}" })
            }
            else -> view(spec)
        }
    }
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
